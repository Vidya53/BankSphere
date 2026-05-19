package com.cts.accountservice.service;

import com.cts.accountservice.client.dto.TransactionRecordRequest;
import com.cts.accountservice.entity.Account;
import com.cts.accountservice.exception.AccountNotActiveException;
import com.cts.accountservice.exception.InsufficientBalanceException;
import com.cts.accountservice.exception.InvalidOperationException;
import com.cts.accountservice.repository.AccountRepository;
import com.cts.accountservice.context.UserContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Cash operations performed by CSRs / Branch Managers at the counter:
 *   - DEPOSIT     → credits a customer's account
 *   - WITHDRAWAL  → debits a customer's account
 *
 * Both are scoped to the staff member's own branch (CSR cannot transact for an
 * account that belongs to another branch). No PIN is required for counter
 * operations — the customer has presented physical ID at the branch.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CashService {

    private final AccountRepository    accountRepository;
    private final TransferService      transferService;     // reuse recordCompletedLedger
    private final NotificationService  notificationService;
    private final AuditService         auditService;

    /** Counter-deposit by CSR — credits the customer's active account. */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Map<String, Object> deposit(String accountNo, BigDecimal amount, String channel,
                                       String description, UserContext csr) {
        validateAmount(amount);
        Account account = mustBeActiveInBranch(accountNo, csr);

        int credited = accountRepository.safeCredit(accountNo, amount);
        if (credited != 1) {
            throw new InvalidOperationException(
                    "Could not credit account — it must be ACTIVE and transactional.");
        }
        BigDecimal newBalance = account.getBalance().add(amount);
        String idempotencyKey = "DEP-" + UUID.randomUUID().toString().substring(0, 12).toUpperCase();

        // Record SUCCESS directly. For DEPOSIT, there is no sender — only receiver.
        String txnId = transferService.recordCompletedLedger(
                TransferService.TransferCommand.builder()
                        .receiverAccountNo(accountNo)
                        .amount(amount)
                        .channel(channel == null ? "CASH" : channel)
                        .description(description == null
                                ? "Counter deposit by " + csr.getUserId()
                                : description)
                        .build(),
                /* sender   */ null,
                /* receiver */ account,
                idempotencyKey,
                "DEPOSIT",
                "SUCCESS",
                /* sender balance after  */ null,
                /* receiver balance after*/ newBalance);

        auditService.logAudit(csr.getUserId(), csr.getRole(), "COUNTER_DEPOSIT",
                "ACCOUNT", accountNo, account.getBranchCode());
        notificationService.sendNotification(account.getCustomerId(), account.getCustomerEmail(),
                "Deposit credited",
                String.format("₹%s deposited at branch %s. New balance: ₹%s",
                        amount, account.getBranchCode(), newBalance));

        return resp("DEPOSIT", idempotencyKey, txnId, newBalance);
    }

    /** Counter-withdrawal by CSR — debits the customer's active account. */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Map<String, Object> withdraw(String accountNo, BigDecimal amount, String channel,
                                        String description, UserContext csr) {
        validateAmount(amount);
        Account account = mustBeActiveInBranch(accountNo, csr);

        // Honour minimumBalance + ACTIVE status atomically
        int debited = accountRepository.debitRespectingMinimum(accountNo, amount);
        if (debited != 1) {
            BigDecimal available = account.getBalance().subtract(
                    account.getMinimumBalance() == null ? BigDecimal.ZERO : account.getMinimumBalance());
            throw new InsufficientBalanceException(
                    "Insufficient balance. Available (after minimum): " + available
                            + ", requested: " + amount);
        }
        BigDecimal newBalance = account.getBalance().subtract(amount);
        String idempotencyKey = "WDR-" + UUID.randomUUID().toString().substring(0, 12).toUpperCase();

        String txnId = transferService.recordCompletedLedger(
                TransferService.TransferCommand.builder()
                        .senderAccountNo(accountNo)
                        .amount(amount)
                        .channel(channel == null ? "CASH" : channel)
                        .description(description == null
                                ? "Counter withdrawal by " + csr.getUserId()
                                : description)
                        .build(),
                /* sender   */ account,
                /* receiver */ null,
                idempotencyKey,
                "WITHDRAWAL",
                "SUCCESS",
                /* sender balance after  */ newBalance,
                /* receiver balance after*/ null);

        auditService.logAudit(csr.getUserId(), csr.getRole(), "COUNTER_WITHDRAWAL",
                "ACCOUNT", accountNo, account.getBranchCode());
        notificationService.sendNotification(account.getCustomerId(), account.getCustomerEmail(),
                "Withdrawal debited",
                String.format("₹%s withdrawn at branch %s. New balance: ₹%s",
                        amount, account.getBranchCode(), newBalance));

        return resp("WITHDRAWAL", idempotencyKey, txnId, newBalance);
    }

    // ── Helpers ──────────────────────────────────────────────────────────────
    private Account mustBeActiveInBranch(String accountNo, UserContext csr) {
        boolean crossBranch = "ADMIN".equals(csr.getRole())
                || csr.getBranchCode() == null
                || csr.getBranchCode().isBlank();
        return (crossBranch
                ? accountRepository.findActiveByAccountNo(accountNo)
                : accountRepository.findActiveByAccountNoAndBranch(accountNo, csr.getBranchCode()))
                .orElseThrow(() -> new AccountNotActiveException(
                        "No active account " + accountNo + " in your branch."));
    }

    private static void validateAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidOperationException("Amount must be greater than zero.");
        }
    }

    private static Map<String, Object> resp(String type, String idem, String txnId, BigDecimal balance) {
        Map<String, Object> out = new HashMap<>();
        out.put("type",            type);
        out.put("status",          "SUCCESS");
        out.put("idempotencyKey",  idem);
        out.put("transactionId",   txnId);
        out.put("balance",         balance);
        return out;
    }
}
