package com.cts.accountservice.controller;

import com.cts.accountservice.dto.response.AccountResponse;
import com.cts.accountservice.dto.response.ApiResponse;
import com.cts.accountservice.service.AccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

/**
 * Internal endpoints for inter-service communication.
 * These are called by transaction-service, loan-service, etc.
 * Secured at gateway level (not JWT) - permitted in SecurityConfig.
 */
@RestController
@RequestMapping("/api/v1/internal/accounts")
@RequiredArgsConstructor
@Tag(name = "Internal - Accounts", description = "Internal endpoints for service-to-service communication")
public class InternalAccountController {

    private final AccountService accountService;

    @GetMapping("/{accountNo}/active")
    @Operation(summary = "Check if account is active")
    public ResponseEntity<ApiResponse<Boolean>> isAccountActive(@PathVariable String accountNo) {
        boolean active = accountService.isAccountActive(accountNo);
        return ResponseEntity.ok(ApiResponse.success("Account status checked", active));
    }

    @GetMapping("/{accountNo}/balance")
    @Operation(summary = "Get account balance")
    public ResponseEntity<ApiResponse<BigDecimal>> getBalance(@PathVariable String accountNo) {
        BigDecimal balance = accountService.getBalance(accountNo);
        return ResponseEntity.ok(ApiResponse.success("Balance retrieved", balance));
    }

    @GetMapping("/{accountNo}")
    @Operation(summary = "Get account details")
    public ResponseEntity<ApiResponse<AccountResponse>> getAccount(@PathVariable String accountNo) {
        AccountResponse response = accountService.getAccountByAccountNo(accountNo);
        return ResponseEntity.ok(ApiResponse.success("Account retrieved", response));
    }

    @PostMapping("/{accountNo}/credit")
    @Operation(summary = "Credit amount to account")
    public ResponseEntity<ApiResponse<Boolean>> credit(@PathVariable String accountNo, @RequestParam BigDecimal amount) {
        boolean result = accountService.creditAccount(accountNo, amount);
        return ResponseEntity.ok(ApiResponse.success("Amount credited successfully", result));
    }

    @PostMapping("/{accountNo}/debit")
    @Operation(summary = "Debit amount from account")
    public ResponseEntity<ApiResponse<Boolean>> debit(@PathVariable String accountNo, @RequestParam BigDecimal amount) {
        boolean result = accountService.debitAccount(accountNo, amount);
        return ResponseEntity.ok(ApiResponse.success("Amount debited successfully", result));
    }
}

