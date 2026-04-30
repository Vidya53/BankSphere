package com.cts.accountservice.controller;

import com.cts.accountservice.dto.response.AccountResponse;
import com.cts.accountservice.dto.response.ApiResponse;
import com.cts.accountservice.security.UserContext;
import com.cts.accountservice.service.AccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/accounts")
@RequiredArgsConstructor
@Tag(name = "Accounts - Customer", description = "Customer account operations")
public class AccountController {

    private final AccountService accountService;

    @GetMapping("/me")
    @Operation(summary = "View my accounts")
    public ResponseEntity<ApiResponse<List<AccountResponse>>> getMyAccounts(
            @RequestHeader(value = "X-User-Id", defaultValue = "CUST001") String userId) {
        UserContext userContext = new UserContext(userId, userId, "CUSTOMER", null, null, null, null);
        List<AccountResponse> accounts = accountService.getMyAccounts(userContext);
        return ResponseEntity.ok(ApiResponse.success("Accounts retrieved successfully", accounts));
    }

    @GetMapping("/{accountNo}")
    @Operation(summary = "View account details by account number")
    public ResponseEntity<ApiResponse<AccountResponse>> getAccountByNo(
            @PathVariable String accountNo) {
        AccountResponse response = accountService.getAccountByAccountNo(accountNo);
        return ResponseEntity.ok(ApiResponse.success("Account retrieved successfully", response));
    }
}
