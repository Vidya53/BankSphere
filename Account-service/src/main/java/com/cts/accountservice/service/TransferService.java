package com.cts.accountservice.service;

import com.cts.accountservice.client.CustomerServiceClient;
import com.cts.accountservice.client.TransactionServiceClient;
import com.cts.accountservice.client.dto.TransactionRecordRequest;
import com.cts.accountservice.entity.Account;
import com.cts.accountservice.entity.PendingTransfer;
import com.cts.accountservice.exception.AccountNotActiveException;
import com.cts.accountservice.exception.InsufficientBalanceException;
import com.cts.accountservice.exception.InvalidOperationException;
import com.cts.accountservice.exception.ResourceNotFoundException;
import com.cts.accountservice.repository.AccountRepository;
import com.cts.accountservice.context.UserContext;
import com.cts.accountservice.repository.PendingTransferRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Customer-facing money-movement service.
 *
 * Flow at a glance:
 *   1.  Validate sender account is ACTIVE and belongs to the calling user.
 *   2.  Verify PIN (locks after 5 failed attempts via PinService).
 *   3.  Validate receiver account is ACTIVE.
 *   4.  Funds check: balance − amount >= minimumBalance (atomic in SQL).
 *   5a. Amount > HIGH_VALUE_THRESHOLD →
 *          enqueue PendingTransfer (status PENDING_APPROVAL); no debit yet.
 *          A CSR approves/rejects from the staff dashboard later.
 *   5b. Amount ≤ threshold → debit + credit immediately, record in ledger.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TransferService {

    @Value("${banking.transfer.highValueThreshold:100000}")
    private BigDecimal highValueThreshold;

    private final AccountRepository         accountRepository;
    private final PendingTransferRepository pendingTransferRepository;
    private final PinService                pinService;
    private final TransactionServiceClient  transactionLedger;
    private final CustomerServiceClient     customerServiceClient;
    private final NotificationService       notificationService;
    private final AuditService              auditService;

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Map<String, Object> transfer(TransferCommand cmd, UserContext ctx) {
        log.info("Transfer request: from {} to {} amount {} initiatedBy={}",
                cmd.getSenderAccountNo(), cmd.getReceiverAccountNo(), cmd.getAmount(), ctx.getUserId());

        // ── 1. Basic validation ───────────────────────────────────────────────
        if (cmd.getAmount() == null || cmd.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidOperationException("Transfer amount must be greater than zero.");
        }
        if (cmd.getSenderAccountNo() == null || cmd.getReceiverAccountNo() == null) {
            throw new InvalidOperationException("Sender and receiver account numbers are required.");
        }
        if (cmd.getSenderAccountNo().equalsIgnoreCase(cmd.getReceiverAccountNo())) {
            throw new InvalidOperationException("Sender and receiver cannot be the same account.");
        }

        // ── 1b. Customer must be ACTIVE — soft-deleted/blocked/inactive
        //         customers cannot move money even if their account row is
        //         still ACTIVE. Account-status is enforced separately below.
        if (!Boolean.TRUE.equals(customerServiceClient.isCustomerActive(ctx.getUserId()))) {
            throw new InvalidOperationException(
                    "Your customer profile is not active. Please contact your branch.");
        }

        // ── 2. Sender must be ACTIVE and owned by the caller ────────────────
        Account sender = accountRepository
                .findActiveOwnedAccount(cmd.getSenderAccountNo(), ctx.getUserId())
                .orElseThrow(() -> new AccountNotActiveException(
                        "Sender account is not active or does not belong to you: " + cmd.getSenderAccountNo()));

        if (!Boolean.TRUE.equals(sender.getIsTransactional())) {
            throw new InvalidOperationException("This account is not enabled for transactions.");
        }

        // ── 3. PIN ───────────────────────────────────────────────────────────
        pinService.verifyPinOrFail(sender, cmd.getPin());

        // ── 4. Receiver must exist and be ACTIVE ────────────────────────────
        Account receiver = accountRepository
                .findActiveByAccountNo(cmd.getReceiverAccountNo())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Receiver account not found or inactive: " + cmd.getReceiverAccountNo()));

        // ── 5. Sufficient funds (respects minimumBalance) ───────────────────
        BigDecimal available = sender.getBalance().subtract(
                sender.getMinimumBalance() == null ? BigDecimal.ZERO : sender.getMinimumBalance());
        if (cmd.getAmount().compareTo(available) > 0) {
            throw new InsufficientBalanceException(
                    "Insufficient balance. Available (after minimum-balance): " + available
                            + ", attempted transfer: " + cmd.getAmount());
        }

        // ── 6. Daily transfer limit ─────────────────────────────────────────
        BigDecimal pendingToday = pendingTransferRepository.sumPendingFromAccountSince(
                sender.getAccountNo(), LocalDateTime.now().toLocalDate().atStartOfDay());
        BigDecimal projectedToday = pendingToday.add(cmd.getAmount());
        if (sender.getDailyTransferLimit() != null
                && projectedToday.compareTo(sender.getDailyTransferLimit()) > 0) {
            throw new InvalidOperationException(
                    "This transfer would exceed your daily transfer limit of "
                            + sender.getDailyTransferLimit()
                            + " (already pending today: " + pendingToday + ").");
        }

        // ── 7. High-value gating: queue for CSR approval ────────────────────
        if (cmd.getAmount().compareTo(highValueThreshold) > 0) {
            return queueForApproval(cmd, sender, receiver, ctx);
        }

        // ── 8. Normal flow: ledger + debit + credit + mark success ──────────
        return executeImmediate(cmd, sender, receiver, ctx, null);
    }

    /**
     * Execute the actual money movement. Called for low-value transfers
     * directly, and by CSR-approval for queued high-value transfers.
     * idempotencyKeyOverride lets the approval flow tie the ledger entry
     * back to the original PendingTransfer reference.
     */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Map<String, Object> executeImmediate(TransferCommand cmd,
                                                Account sender,
                                                Account receiver,
                                                UserContext ctx,
                                                String idempotencyKeyOverride) {

        String idempotencyKey = idempotencyKeyOverride != null
                ? idempotencyKeyOverride
                : ("TRF-" + UUID.randomUUID().toString().substring(0, 12).toUpperCase());

        // 1. Debit sender atomically with minimum-balance check
        int debited = accountRepository.debitRespectingMinimum(
                sender.getAccountNo(), cmd.getAmount());
        if (debited != 1) {
            throw new InsufficientBalanceException(
                    "Could not debit account — funds may have changed since the check.");
        }

        // 2. Credit receiver
        int credited = accountRepository.creditAccount(
                receiver.getAccountNo(), cmd.getAmount());
        if (credited != 1) {
            // Rollback the debit in this same transaction
            accountRepository.creditAccount(sender.getAccountNo(), cmd.getAmount());
            throw new InvalidOperationException(
                    "Could not credit receiver. Your account has not been debited.");
        }

        BigDecimal newSenderBalance   = sender.getBalance().subtract(cmd.getAmount());
        BigDecimal newReceiverBalance = receiver.getBalance().add(cmd.getAmount());

        // 3. Record the transaction as SUCCESS in one shot.
        // Previously we did initiate-as-PENDING then markSuccess, which left
        // transactions stuck at PENDING when the second call silently failed.
        String txnId = recordCompletedLedger(cmd, sender, receiver, idempotencyKey,
                "TRANSFER", "SUCCESS", newSenderBalance, newReceiverBalance);

        // 4. Side-effects
        auditService.logAudit(ctx.getUserId(), ctx.getRole(), "TRANSFER_COMPLETED",
                "ACCOUNT", sender.getAccountNo(), sender.getBranchCode());
        notificationService.sendNotification(sender.getCustomerId(), sender.getCustomerEmail(),
                "Transfer Successful",
                String.format("Transferred ₹%s from %s to %s. Reference: %s",
                        cmd.getAmount(), sender.getAccountNo(), receiver.getAccountNo(), idempotencyKey));

        Map<String, Object> out = new HashMap<>();
        out.put("status", "SUCCESS");
        out.put("idempotencyKey", idempotencyKey);
        out.put("transactionId",  txnId);
        out.put("senderBalance",  newSenderBalance);
        out.put("receiverBalance", newReceiverBalance);
        return out;
    }

    /**
     * Records a finalised transaction in the ledger with status SUCCESS in a
     * single call (rather than initiate-then-markSuccess). Used for transfers,
     * deposits and withdrawals once the actual funds movement has completed.
     */
    public String recordCompletedLedger(TransferCommand cmd, Account sender, Account receiver,
                                        String idempotencyKey, String transactionType,
                                        String initialStatus,
                                        BigDecimal newSenderBalance,
                                        BigDecimal newReceiverBalance) {
        try {
            Map<String, Object> res = transactionLedger.initiate(TransactionRecordRequest.builder()
                    .senderAccountId(sender != null ? sender.getAccountNo() : null)
                    .receiverAccountId(receiver != null ? receiver.getAccountNo() : null)
                    .amount(cmd.getAmount())
                    .currency("INR")
                    .transactionType(transactionType)
                    .channel(cmd.getChannel() == null ? "INTERNAL" : cmd.getChannel())
                    .idempotencyKey(idempotencyKey)
                    .description(cmd.getDescription())
                    .initialStatus(initialStatus)
                    .senderBalanceAfter(newSenderBalance)
                    .receiverBalanceAfter(newReceiverBalance)
                    .build());
            Object data = res.get("data");
            if (data instanceof Map<?, ?> m) {
                Object id = m.get("transactionId");
                if (id != null) return id.toString();
            }
        } catch (Exception e) {
            log.warn("Ledger record failed for {}: {}", idempotencyKey, e.getMessage());
        }
        return null;
    }

    private Map<String, Object> queueForApproval(TransferCommand cmd,
                                                 Account sender,
                                                 Account receiver,
                                                 UserContext ctx) {
        String ref = "PTR-" + LocalDateTime.now().toLocalDate().toString().replace("-", "")
                + "-" + String.valueOf(System.nanoTime()).substring(0, 7);

        PendingTransfer pt = PendingTransfer.builder()
                .reference(ref)
                .senderAccountNo(sender.getAccountNo())
                .receiverAccountNo(receiver.getAccountNo())
                .amount(cmd.getAmount())
                .branchCode(sender.getBranchCode())
                .channel(cmd.getChannel())
                .description(cmd.getDescription())
                .initiatedBy(ctx.getUserId())
                .initiatedByName(sender.getCustomerName())
                .status(PendingTransfer.Status.PENDING_APPROVAL)
                .build();
        pendingTransferRepository.save(pt);

        auditService.logAudit(ctx.getUserId(), ctx.getRole(),
                "TRANSFER_QUEUED_FOR_APPROVAL",
                "PENDING_TRANSFER", ref, sender.getBranchCode());
        notificationService.sendNotification(sender.getCustomerId(), sender.getCustomerEmail(),
                "Transfer pending CSR approval",
                String.format("Your high-value transfer of ₹%s requires CSR approval. Reference: %s",
                        cmd.getAmount(), ref));

        Map<String, Object> out = new HashMap<>();
        out.put("status",    "PENDING_APPROVAL");
        out.put("reference", ref);
        out.put("amount",    cmd.getAmount());
        out.put("threshold", highValueThreshold);
        out.put("message",   "This transfer exceeds the high-value threshold of ₹"
                + highValueThreshold + " and is awaiting CSR approval at branch "
                + sender.getBranchCode() + ".");
        return out;
    }

    // ── Command DTO ──────────────────────────────────────────────────────────
    @lombok.Getter @lombok.Setter @lombok.NoArgsConstructor @lombok.AllArgsConstructor @lombok.Builder
    public static class TransferCommand {
        private String     senderAccountNo;
        private String     receiverAccountNo;
        private BigDecimal amount;
        private String     pin;
        private String     channel;
        private String     description;
    }
}
