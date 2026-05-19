package com.cts.accountservice.controller;

import com.cts.accountservice.dto.response.ApiResponse;
import com.cts.accountservice.entity.PendingTransfer;
import com.cts.accountservice.repository.PendingTransferRepository;
import com.cts.accountservice.context.UserContext;
import com.cts.accountservice.context.UserContextExtractor;
import com.cts.accountservice.service.TransferService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/transfers")
@RequiredArgsConstructor
@PreAuthorize("hasRole('CUSTOMER')")
@Tag(name = "Transfers · Customer", description = "Customer-initiated money transfers (PIN-protected; high-value transfers queue for CSR approval)")
public class TransferController {

    private final TransferService           transferService;
    private final UserContextExtractor      userContextExtractor;
    private final PendingTransferRepository pendingRepository;

    @PostMapping
    @Operation(
            summary = "Initiate a transfer",
            description = """
                    Validates the transaction PIN, account/customer status and available funds, then either executes the transfer immediately or — if the amount exceeds ₹1,00,000 — queues it as `PENDING_APPROVAL` for CSR review in the sender's branch. Funds are not debited for queued transfers.

                    **Allowed roles:** CUSTOMER
                    **Side effects:** May move funds; emits audit events on `banking.audit.events` and notifications on `banking.notification.events`."""
    )
    public ResponseEntity<ApiResponse<Map<String, Object>>> initiate(
            @Valid @RequestBody InitiateTransferRequest request,
            HttpServletRequest http) {

        UserContext ctx = userContextExtractor.extract(http);
        Map<String, Object> result = transferService.transfer(
                TransferService.TransferCommand.builder()
                        .senderAccountNo(request.getSenderAccountNo())
                        .receiverAccountNo(request.getReceiverAccountNo())
                        .amount(request.getAmount())
                        .pin(request.getPin())
                        .channel(request.getChannel())
                        .description(request.getDescription())
                        .build(),
                ctx);
        String message = "PENDING_APPROVAL".equals(result.get("status"))
                ? "Transfer queued for CSR approval"
                : "Transfer completed";
        return ResponseEntity.ok(ApiResponse.success(message, result));
    }

    @GetMapping("/pending/me")
    @Operation(
            summary = "List my pending high-value transfers",
            description = """
                    Returns transfers that the calling customer initiated but are still awaiting branch (CSR/BM) approval, ordered newest-first.

                    **Allowed roles:** CUSTOMER"""
    )
    public ResponseEntity<ApiResponse<List<PendingTransfer>>> myPending(HttpServletRequest http) {
        UserContext ctx = userContextExtractor.extract(http);
        List<PendingTransfer> mine = pendingRepository.findByInitiatedByOrderByCreatedAtDesc(ctx.getUserId());
        return ResponseEntity.ok(ApiResponse.success(
                "Your transfers awaiting approval: " + mine.size(), mine));
    }

    // ── Request DTO ──────────────────────────────────────────────────────────
    @Getter @Setter @NoArgsConstructor
    public static class InitiateTransferRequest {

        @NotBlank(message = "Sender account number is required")
        @Pattern(regexp = "^[A-Z]{3}[A-Z0-9]{14}$", message = "Sender account number must be a 3-letter prefix followed by 14 alphanumerics")
        private String senderAccountNo;

        @NotBlank(message = "Receiver account number is required")
        @Pattern(regexp = "^[A-Z]{3}[A-Z0-9]{14}$", message = "Receiver account number must be a 3-letter prefix followed by 14 alphanumerics")
        private String receiverAccountNo;

        @NotNull(message = "Transfer amount is required")
        @DecimalMin(value = "0.01", message = "Amount must be greater than zero")
        @Digits(integer = 13, fraction = 2, message = "Amount supports up to 13 integer and 2 fractional digits")
        private BigDecimal amount;

        @NotBlank(message = "Transaction PIN is required")
        @Pattern(regexp = "^[0-9]{4,6}$", message = "PIN must be 4 to 6 digits")
        private String pin;

        @Pattern(regexp = "^$|^(UPI|NEFT|IMPS|RTGS|INTERNAL|BRANCH|ATM|MOBILE_APP|NET_BANKING|API)$", message = "Channel must be one of UPI, NEFT, IMPS, RTGS, INTERNAL, BRANCH, ATM, MOBILE_APP, NET_BANKING or API")
        private String channel;

        @Size(max = 255, message = "Description must not exceed 255 characters")
        private String description;
    }
}
