package com.cts.accountservice.controller;

import com.cts.accountservice.dto.response.AccountResponse;
import com.cts.accountservice.dto.response.ApiResponse;
import com.cts.accountservice.context.UserContext;
import com.cts.accountservice.context.UserContextExtractor;
import com.cts.accountservice.service.AccountService;
import com.cts.accountservice.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/accounts")
@RequiredArgsConstructor
@Tag(name = "Accounts · Customer", description = "Customer-facing account read and notification endpoints")
public class AccountController {

    private final AccountService accountService;
    private final UserContextExtractor userContextExtractor;
    private final NotificationService notificationService;

    @GetMapping("/me")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(
            summary = "List my accounts",
            description = """
                    Returns every account (ACTIVE, FROZEN or CLOSED) owned by the authenticated customer, identified by the gateway-injected `X-User-Id` header.

                    **Allowed roles:** CUSTOMER"""
    )
    public ResponseEntity<ApiResponse<List<AccountResponse>>> getMyAccounts(HttpServletRequest request) {
        UserContext ctx = userContextExtractor.extract(request);
        List<AccountResponse> accounts = accountService.getMyAccounts(ctx);
        return ResponseEntity.ok(ApiResponse.success("Accounts retrieved successfully", accounts));
    }

    @GetMapping("/{accountNo}")
    @PreAuthorize("hasAnyRole('CUSTOMER','CSR','BRANCH_MANAGER','ADMIN')")
    @Operation(
            summary = "Get account details by account number",
            description = """
                    Fetches the full account record for the supplied account number, including balance, status (ACTIVE/FROZEN/CLOSED), customer linkage and branch.

                    **Allowed roles:** CUSTOMER, CSR, BRANCH_MANAGER, ADMIN"""
    )
    public ResponseEntity<ApiResponse<AccountResponse>> getAccountByNo(@PathVariable String accountNo) {
        AccountResponse response = accountService.getAccountByAccountNo(accountNo);
        return ResponseEntity.ok(ApiResponse.success("Account retrieved successfully", response));
    }

    @PostMapping("/{accountNo}/statement-notify")
    @PreAuthorize("hasAnyRole('CUSTOMER','CSR','BRANCH_MANAGER','ADMIN')")
    @Operation(
            summary = "Notify customer of statement download",
            description = """
                    Sends an email to the account holder confirming that a statement (optionally for a given date range) was just downloaded from the portal. Silently no-ops when no email is on file.

                    **Allowed roles:** CUSTOMER, CSR, BRANCH_MANAGER, ADMIN
                    **Side effects:** Publishes an event on `banking.notification.events`."""
    )
    public ResponseEntity<ApiResponse<Void>> notifyStatementDownloaded(
            @PathVariable String accountNo,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            HttpServletRequest request) {

        UserContext ctx = userContextExtractor.extract(request);
        AccountResponse account = accountService.getAccountByAccountNo(accountNo);

        if (ctx.getEmail() == null || ctx.getEmail().isBlank()) {
            log.warn("Statement-notify skipped — no email in gateway headers for userId={}", ctx.getUserId());
            return ResponseEntity.ok(ApiResponse.success("Statement download recorded (no email on file)"));
        }

        String greetingName = ctx.getCustomerName() != null && !ctx.getCustomerName().isBlank()
                ? ctx.getCustomerName()
                : "Customer";
        String range = (from != null && !from.isBlank() && to != null && !to.isBlank())
                ? " for the period " + from + " to " + to
                : "";
        String subject = "Your BankSphere account statement was downloaded";
        String message = String.format(
                "Hi %s,%n%n" +
                "A statement for your %s account ending %s%s was just downloaded from your BankSphere portal.%n%n" +
                "If this wasn't you, please contact our support team immediately and review your account activity.%n%n" +
                "Thank you for banking with BankSphere.",
                greetingName,
                account.getAccountType(),
                maskAccountNo(accountNo),
                range);

        notificationService.sendNotification(ctx.getUserId(), ctx.getEmail(), subject, message);
        log.info("Statement-download notification queued: accountNo={} userId={}", accountNo, ctx.getUserId());
        return ResponseEntity.ok(ApiResponse.success("Statement download notification queued"));
    }

    private String maskAccountNo(String accountNo) {
        if (accountNo == null || accountNo.length() < 4) return accountNo;
        return "••••" + accountNo.substring(accountNo.length() - 4);
    }
}
