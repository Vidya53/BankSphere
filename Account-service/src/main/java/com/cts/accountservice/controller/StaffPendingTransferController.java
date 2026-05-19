package com.cts.accountservice.controller;

import com.cts.accountservice.dto.response.ApiResponse;
import com.cts.accountservice.entity.Account;
import com.cts.accountservice.entity.PendingTransfer;
import com.cts.accountservice.exception.InvalidOperationException;
import com.cts.accountservice.exception.ResourceNotFoundException;
import com.cts.accountservice.repository.AccountRepository;
import com.cts.accountservice.repository.PendingTransferRepository;
import com.cts.accountservice.context.UserContext;
import com.cts.accountservice.context.UserContextExtractor;
import com.cts.accountservice.service.AuditService;
import com.cts.accountservice.service.NotificationService;
import com.cts.accountservice.service.TransferService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * CSR / Branch Manager review queue for high-value transfers
 * (above the configured threshold). Approval triggers the actual debit + credit
 * via TransferService; rejection leaves balances untouched.
 */
@RestController
@RequestMapping("/api/v1/staff/pending-transfers")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('CSR','BRANCH_MANAGER','ADMIN')")
@Tag(name = "Pending High-Value Transfers · Staff", description = "Branch review queue for transfers above ₹1,00,000 awaiting CSR/BM approval")
public class StaffPendingTransferController {

    private final PendingTransferRepository pendingRepository;
    private final AccountRepository         accountRepository;
    private final TransferService           transferService;
    private final NotificationService       notificationService;
    private final AuditService               auditService;
    private final UserContextExtractor       userContextExtractor;

    @GetMapping
    @Operation(
            summary = "List pending high-value transfers",
            description = """
                    Returns transfers in `PENDING_APPROVAL` status scoped to the staff member's branch (the sender's branch). When no branch header is present, returns the bank-wide queue for admin-style callers.

                    **Allowed roles:** CSR, BRANCH_MANAGER, ADMIN"""
    )
    public ResponseEntity<ApiResponse<List<PendingTransfer>>> list(HttpServletRequest http) {
        UserContext ctx = userContextExtractor.extract(http);
        List<PendingTransfer> rows = ctx.getBranchCode() == null || ctx.getBranchCode().isBlank()
                ? pendingRepository.findByStatusOrderByCreatedAtAsc(PendingTransfer.Status.PENDING_APPROVAL)
                : pendingRepository.findByBranchCodeAndStatusOrderByCreatedAtAsc(
                        ctx.getBranchCode(), PendingTransfer.Status.PENDING_APPROVAL);
        return ResponseEntity.ok(ApiResponse.success(
                "Pending high-value transfers: " + rows.size(), rows));
    }

    @PostMapping("/{reference}/approve")
    @Operation(
            summary = "Approve and execute a pending transfer",
            description = """
                    Approves the pending transfer and triggers the actual debit/credit via `TransferService`. The branch check ensures CSR/BM can only approve transfers from their own branch; ADMIN bypasses it.

                    **Allowed roles:** CSR, BRANCH_MANAGER, ADMIN
                    **Side effects:** Moves funds; emits audit event `HIGH_VALUE_TRANSFER_APPROVED` on `banking.audit.events` and notification on `banking.notification.events`."""
    )
    @Transactional
    public ResponseEntity<ApiResponse<Map<String, Object>>> approve(
            @PathVariable String reference,
            HttpServletRequest http) {

        UserContext ctx = userContextExtractor.extract(http);
        PendingTransfer pt = pendingRepository.findByReference(reference)
                .orElseThrow(() -> new ResourceNotFoundException("Pending transfer not found: " + reference));

        ensureSameBranch(ctx, pt);
        ensureStillPending(pt);

        // Look up both accounts fresh so the executor sees current balances.
        Account sender = accountRepository.findActiveByAccountNo(pt.getSenderAccountNo())
                .orElseThrow(() -> new InvalidOperationException(
                        "Sender account is no longer active: " + pt.getSenderAccountNo()));
        Account receiver = accountRepository.findActiveByAccountNo(pt.getReceiverAccountNo())
                .orElseThrow(() -> new InvalidOperationException(
                        "Receiver account is no longer active: " + pt.getReceiverAccountNo()));

        // Build a UserContext that pretends to be the original sender for the
        // audit/notification side-effects produced by executeImmediate.
        UserContext senderCtx = new UserContext(
                pt.getInitiatedBy(),
                pt.getInitiatedByName(),
                "CUSTOMER",
                sender.getBranchCode(),
                pt.getInitiatedByName(),
                sender.getCustomerEmail(),
                sender.getCustomerPhone());

        TransferService.TransferCommand cmd = TransferService.TransferCommand.builder()
                .senderAccountNo(pt.getSenderAccountNo())
                .receiverAccountNo(pt.getReceiverAccountNo())
                .amount(pt.getAmount())
                .channel(pt.getChannel())
                .description(pt.getDescription() == null
                        ? "Transfer approved by CSR (" + reference + ")"
                        : pt.getDescription())
                .build();

        Map<String, Object> result = transferService.executeImmediate(cmd, sender, receiver, senderCtx, reference);

        pt.setStatus(PendingTransfer.Status.APPROVED);
        pt.setReviewedBy(ctx.getUserId());
        pt.setReviewedAt(LocalDateTime.now());
        pt.setGeneratedTransactionRef(String.valueOf(result.get("idempotencyKey")));
        pendingRepository.save(pt);

        auditService.logAudit(ctx.getUserId(), ctx.getRole(), "HIGH_VALUE_TRANSFER_APPROVED",
                "PENDING_TRANSFER", reference, pt.getBranchCode());
        notificationService.sendNotification(sender.getCustomerId(), sender.getCustomerEmail(),
                "Transfer approved",
                String.format("Your transfer of ₹%s (%s) was approved by the branch and has been processed.",
                        pt.getAmount(), reference));

        return ResponseEntity.ok(ApiResponse.success(
                "Transfer approved and executed", result));
    }

    @PostMapping("/{reference}/reject")
    @Operation(
            summary = "Reject a pending transfer",
            description = """
                    Marks the pending transfer `REJECTED` with the supplied reason. Funds are never debited so balances remain untouched.

                    **Allowed roles:** CSR, BRANCH_MANAGER, ADMIN
                    **Side effects:** Emits audit event `HIGH_VALUE_TRANSFER_REJECTED` on `banking.audit.events` and a rejection notification on `banking.notification.events`."""
    )
    @Transactional
    public ResponseEntity<ApiResponse<PendingTransfer>> reject(
            @PathVariable String reference,
            @Valid @RequestBody RejectRequest body,
            HttpServletRequest http) {

        UserContext ctx = userContextExtractor.extract(http);
        PendingTransfer pt = pendingRepository.findByReference(reference)
                .orElseThrow(() -> new ResourceNotFoundException("Pending transfer not found: " + reference));

        ensureSameBranch(ctx, pt);
        ensureStillPending(pt);

        pt.setStatus(PendingTransfer.Status.REJECTED);
        pt.setReviewedBy(ctx.getUserId());
        pt.setReviewedAt(LocalDateTime.now());
        pt.setRejectionReason(body.getReason());
        pendingRepository.save(pt);

        auditService.logAudit(ctx.getUserId(), ctx.getRole(), "HIGH_VALUE_TRANSFER_REJECTED",
                "PENDING_TRANSFER", reference, pt.getBranchCode());

        // Look up the sender's email for a friendly notification
        accountRepository.findByAccountNo(pt.getSenderAccountNo()).ifPresent(s ->
                notificationService.sendNotification(s.getCustomerId(), s.getCustomerEmail(),
                        "Transfer rejected",
                        String.format("Your transfer of ₹%s (%s) was rejected by the branch. Reason: %s",
                                pt.getAmount(), reference, body.getReason())));

        return ResponseEntity.ok(ApiResponse.success("Transfer rejected", pt));
    }

    private void ensureSameBranch(UserContext ctx, PendingTransfer pt) {
        if (ctx.getBranchCode() == null) return;          // Admin / cross-branch staff
        if ("ADMIN".equals(ctx.getRole()))  return;       // Admins bypass
        if (!ctx.getBranchCode().equals(pt.getBranchCode())) {
            throw new InvalidOperationException(
                    "You can only review transfers from your own branch (" + ctx.getBranchCode() + ").");
        }
    }

    private static void ensureStillPending(PendingTransfer pt) {
        if (pt.getStatus() != PendingTransfer.Status.PENDING_APPROVAL) {
            throw new InvalidOperationException(
                    "This transfer is no longer pending. Current status: " + pt.getStatus());
        }
    }

    @Getter @Setter @NoArgsConstructor
    public static class RejectRequest {
        @NotBlank @Size(min = 6, max = 500, message = "Reason must be 6–500 characters")
        private String reason;
    }
}
