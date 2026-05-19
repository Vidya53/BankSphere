package com.cts.accountservice.controller;

import com.cts.accountservice.dto.response.ApiResponse;
import com.cts.accountservice.context.UserContext;
import com.cts.accountservice.context.UserContextExtractor;
import com.cts.accountservice.service.CashService;
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
import java.util.Map;

/**
 * Counter-cash operations exposed to CSRs / Branch Managers / Admin.
 *   POST /api/v1/staff/cash/deposit
 *   POST /api/v1/staff/cash/withdrawal
 *
 * The staff member is identified via X-User-Id (injected by the gateway from
 * the JWT). All operations are scoped to the staff member's own branch
 * (Admins bypass the branch check).
 */
@RestController
@RequestMapping("/api/v1/staff/cash")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('CSR','BRANCH_MANAGER','ADMIN')")
@Tag(name = "Cash Counter · Staff", description = "Counter cash operations (CSR/BM act on their own branch; ADMIN bypasses scope)")
public class StaffCashController {

    private final CashService          cashService;
    private final UserContextExtractor userContextExtractor;

    @PostMapping("/deposit")
    @Operation(
            summary = "Counter deposit (credit account)",
            description = """
                    Credits cash to a customer's account at the branch counter. CSR and BRANCH_MANAGER may only act on accounts that belong to their own branch; ADMIN bypasses the branch scope.

                    **Allowed roles:** CSR, BRANCH_MANAGER, ADMIN
                    **Side effects:** Updates balance; emits a transaction record, audit event on `banking.audit.events` and a notification on `banking.notification.events`."""
    )
    public ResponseEntity<ApiResponse<Map<String, Object>>> deposit(
            @Valid @RequestBody CashRequest body,
            HttpServletRequest http) {
        UserContext ctx = userContextExtractor.extract(http);
        Map<String, Object> result = cashService.deposit(
                body.getAccountNo(), body.getAmount(), body.getChannel(), body.getDescription(), ctx);
        return ResponseEntity.ok(ApiResponse.success("Deposit credited", result));
    }

    @PostMapping("/withdrawal")
    @Operation(
            summary = "Counter withdrawal (debit account)",
            description = """
                    Debits cash from a customer's account at the branch counter after a sufficient-funds check. Branch scope applies to CSR/BRANCH_MANAGER; ADMIN bypasses it.

                    **Allowed roles:** CSR, BRANCH_MANAGER, ADMIN
                    **Side effects:** Updates balance; emits a transaction record, audit event on `banking.audit.events` and a notification on `banking.notification.events`."""
    )
    public ResponseEntity<ApiResponse<Map<String, Object>>> withdraw(
            @Valid @RequestBody CashRequest body,
            HttpServletRequest http) {
        UserContext ctx = userContextExtractor.extract(http);
        Map<String, Object> result = cashService.withdraw(
                body.getAccountNo(), body.getAmount(), body.getChannel(), body.getDescription(), ctx);
        return ResponseEntity.ok(ApiResponse.success("Withdrawal debited", result));
    }

    @Getter @Setter @NoArgsConstructor
    public static class CashRequest {
        @NotBlank(message = "Account number is required")
        @Pattern(regexp = "^[A-Z]{3}[A-Z0-9]{14}$", message = "Account number must be a 3-letter prefix followed by 14 alphanumerics")
        private String accountNo;

        @NotNull(message = "Amount is required")
        @DecimalMin(value = "0.01", message = "Amount must be greater than zero")
        @Digits(integer = 13, fraction = 2, message = "Amount supports up to 13 integer and 2 fractional digits")
        private BigDecimal amount;

        @Pattern(regexp = "^$|^(CASH|UPI|NEFT|IMPS|RTGS|INTERNAL|BRANCH|ATM|MOBILE_APP|NET_BANKING|API)$", message = "Channel must be one of CASH, UPI, NEFT, IMPS, RTGS, INTERNAL, BRANCH, ATM, MOBILE_APP, NET_BANKING or API")
        private String channel;

        @Size(max = 255, message = "Description must not exceed 255 characters")
        private String description;
    }
}
