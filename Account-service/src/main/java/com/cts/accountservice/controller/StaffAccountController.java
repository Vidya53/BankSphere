package com.cts.accountservice.controller;

import com.cts.accountservice.dto.request.CloseAccountRequest;
import com.cts.accountservice.dto.request.FreezeAccountRequest;
import com.cts.accountservice.dto.response.AccountResponse;
import com.cts.accountservice.dto.response.ApiResponse;
import com.cts.accountservice.dto.response.BranchAccountSummary;
import com.cts.accountservice.security.UserContext;
import com.cts.accountservice.service.AccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/staff/accounts")
@RequiredArgsConstructor
@Tag(name = "Accounts - Staff", description = "Staff account management operations")
public class StaffAccountController {

    private final AccountService accountService;

    @GetMapping("/my-branch")
    @PreAuthorize("hasAnyRole('CSR', 'BRANCH_MANAGER')")
    @Operation(summary = "View branch accounts", description = "Filter by status: ACTIVE, FROZEN, CLOSED, DORMANT")
    public ResponseEntity<ApiResponse<List<AccountResponse>>> getBranchAccounts(
            @RequestParam(required = false) String status,
            @AuthenticationPrincipal UserContext userContext) {
        List<AccountResponse> accounts = accountService.getBranchAccounts(userContext.getBranchCode(), status);
        return ResponseEntity.ok(ApiResponse.success("Branch accounts retrieved", accounts));
    }

    @GetMapping("/my-branch/summary")
    @PreAuthorize("hasAnyRole('CSR', 'BRANCH_MANAGER')")
    @Operation(summary = "View branch account summary/KPIs")
    public ResponseEntity<ApiResponse<BranchAccountSummary>> getBranchSummary(
            @AuthenticationPrincipal UserContext userContext) {
        BranchAccountSummary summary = accountService.getBranchSummary(userContext.getBranchCode());
        return ResponseEntity.ok(ApiResponse.success("Branch summary retrieved", summary));
    }

    @PatchMapping("/{accountNo}/freeze")
    @PreAuthorize("hasAnyRole('CSR', 'BRANCH_MANAGER')")
    @Operation(summary = "Freeze a customer account")
    public ResponseEntity<ApiResponse<AccountResponse>> freezeAccount(
            @PathVariable String accountNo,
            @Valid @RequestBody FreezeAccountRequest request,
            @AuthenticationPrincipal UserContext userContext) {
        AccountResponse response = accountService.freezeAccount(accountNo, request, userContext);
        return ResponseEntity.ok(ApiResponse.success("Account frozen successfully", response));
    }

    @PatchMapping("/{accountNo}/unfreeze")
    @PreAuthorize("hasAnyRole('CSR', 'BRANCH_MANAGER')")
    @Operation(summary = "Unfreeze a customer account")
    public ResponseEntity<ApiResponse<AccountResponse>> unfreezeAccount(
            @PathVariable String accountNo,
            @AuthenticationPrincipal UserContext userContext) {
        AccountResponse response = accountService.unfreezeAccount(accountNo, userContext);
        return ResponseEntity.ok(ApiResponse.success("Account unfrozen successfully", response));
    }

    @PatchMapping("/{accountNo}/close")
    @PreAuthorize("hasAnyRole('BRANCH_MANAGER')")
    @Operation(summary = "Close a customer account", description = "Only Branch Manager can close accounts. Balance must be zero.")
    public ResponseEntity<ApiResponse<AccountResponse>> closeAccount(
            @PathVariable String accountNo,
            @Valid @RequestBody CloseAccountRequest request,
            @AuthenticationPrincipal UserContext userContext) {
        AccountResponse response = accountService.closeAccount(accountNo, request, userContext);
        return ResponseEntity.ok(ApiResponse.success("Account closed successfully", response));
    }
}

