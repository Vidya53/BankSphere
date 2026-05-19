package com.cts.accountservice.controller;

import com.cts.accountservice.dto.response.AccountResponse;
import com.cts.accountservice.dto.response.ApiResponse;
import com.cts.accountservice.service.AccountService;
import com.cts.accountservice.service.PinService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Internal endpoints for inter-service communication.
 * These are called by transaction-service, loan-service, etc.
 * Secured at gateway level (not JWT) - permitted in SecurityConfig.
 */
@RestController
@RequestMapping("/api/v1/internal/accounts")
@RequiredArgsConstructor
@Tag(name = "Internal · Accounts", description = "Service-to-service account operations (Feign-only; blocked at the API gateway)")
public class InternalAccountController {

    private final AccountService accountService;
    private final PinService pinService;

    @GetMapping("/{accountNo}/active")
    @Operation(
            summary = "Check whether an account is active",
            description = """
                    Returns `true` when the account exists and is in `ACTIVE` status (i.e. not FROZEN/CLOSED). Used by transaction-service and loan-service before issuing debits.

                    **Allowed roles:** Internal — blocked at the API Gateway; callable only by other services via Feign."""
    )
    public ResponseEntity<ApiResponse<Boolean>> isAccountActive(@PathVariable String accountNo) {
        boolean active = accountService.isAccountActive(accountNo);
        return ResponseEntity.ok(ApiResponse.success("Account status checked", active));
    }

    @GetMapping("/{accountNo}/balance")
    @Operation(
            summary = "Fetch current account balance",
            description = """
                    Returns the live balance for the account. Used by callers that need a numeric balance check without the full account payload.

                    **Allowed roles:** Internal — blocked at the API Gateway; callable only by other services via Feign."""
    )
    public ResponseEntity<ApiResponse<BigDecimal>> getBalance(@PathVariable String accountNo) {
        BigDecimal balance = accountService.getBalance(accountNo);
        return ResponseEntity.ok(ApiResponse.success("Balance retrieved", balance));
    }

    @GetMapping("/{accountNo}")
    @Operation(
            summary = "Fetch full account details",
            description = """
                    Returns the full account record (status, balance, customer linkage, branch) for use by sibling services.

                    **Allowed roles:** Internal — blocked at the API Gateway; callable only by other services via Feign."""
    )
    public ResponseEntity<ApiResponse<AccountResponse>> getAccount(@PathVariable String accountNo) {
        AccountResponse response = accountService.getAccountByAccountNo(accountNo);
        return ResponseEntity.ok(ApiResponse.success("Account retrieved", response));
    }

    @PostMapping("/{accountNo}/credit")
    @Operation(
            summary = "Credit an amount to the account",
            description = """
                    Increases the account balance by the supplied amount. Used by transaction-service and loan-service to post settlement credits.

                    **Allowed roles:** Internal — blocked at the API Gateway; callable only by other services via Feign.
                    **Side effects:** Persists a balance change; downstream services typically emit their own audit events."""
    )
    public ResponseEntity<ApiResponse<Boolean>> credit(@PathVariable String accountNo, @RequestParam BigDecimal amount) {
        boolean result = accountService.creditAccount(accountNo, amount);
        return ResponseEntity.ok(ApiResponse.success("Amount credited successfully", result));
    }

    @PostMapping("/{accountNo}/debit")
    @Operation(
            summary = "Debit an amount from the account",
            description = """
                    Decreases the account balance by the supplied amount after validating sufficient funds. Caller is responsible for any PIN check upstream.

                    **Allowed roles:** Internal — blocked at the API Gateway; callable only by other services via Feign.
                    **Side effects:** Persists a balance change."""
    )
    public ResponseEntity<ApiResponse<Boolean>> debit(@PathVariable String accountNo, @RequestParam BigDecimal amount) {
        boolean result = accountService.debitAccount(accountNo, amount);
        return ResponseEntity.ok(ApiResponse.success("Amount debited successfully", result));
    }

    @PostMapping("/customer/{customerId}/close-all")
    @Operation(
            summary = "Cascade-close all accounts of a customer",
            description = """
                    Closes every account owned by the customer (balances must already be zero). Invoked by `customer-service` when a customer is soft-deleted.

                    **Allowed roles:** Internal — blocked at the API Gateway; callable only by other services via Feign.
                    **Side effects:** Transitions accounts to `CLOSED`; emits audit events on `banking.audit.events`."""
    )
    public ResponseEntity<ApiResponse<Integer>> closeAllAccountsForCustomer(
            @PathVariable String customerId,
            @RequestParam(defaultValue = "Customer soft-deleted") String reason,
            @RequestParam(defaultValue = "SYSTEM") String closedBy) {
        int closed = accountService.closeAllAccountsForCustomer(customerId, reason, closedBy);
        return ResponseEntity.ok(ApiResponse.success("Closed " + closed + " accounts", closed));
    }

    /**
     * PIN-verified debit. Used by loan-service for EMI payments and prepayments
     * so the customer's transaction PIN is enforced the same way it is for
     * transfers. PinService runs the same lockout + attempt-counter logic.
     *
     * The PIN is verified first; the debit only fires if verification succeeds.
     * Both run in the same transaction so a successful debit can't happen
     * against a wrong PIN.
     */
    @PostMapping("/{accountNo}/debit-with-pin")
    @Operation(
            summary = "Atomic PIN-verified debit",
            description = """
                    Verifies the customer's transaction PIN and, only on success, debits the requested amount in the same transaction. Used by loan-service for EMI deductions and prepayments. Enforces the same lockout policy as transfers (5 wrong attempts → 15-minute lock).

                    **Allowed roles:** Internal — blocked at the API Gateway; callable only by other services via Feign.
                    **Side effects:** Persists a balance change; increments PIN attempt counter on failure."""
    )
    public ResponseEntity<ApiResponse<Boolean>> debitWithPin(
            @PathVariable String accountNo,
            @RequestBody Map<String, Object> body) {

        String pin = body.get("pin") == null ? "" : body.get("pin").toString();
        Object amt = body.get("amount");
        if (amt == null) {
            throw new com.cts.accountservice.exception.InvalidOperationException("Amount is required");
        }
        BigDecimal amount = new BigDecimal(amt.toString());

        pinService.verifyPinForAccount(accountNo, pin);
        boolean result = accountService.debitAccount(accountNo, amount);
        return ResponseEntity.ok(ApiResponse.success("Amount debited after PIN verification", result));
    }
}

