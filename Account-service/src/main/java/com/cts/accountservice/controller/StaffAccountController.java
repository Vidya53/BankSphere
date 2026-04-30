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
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/staff/accounts")
@RequiredArgsConstructor
@Tag(name = "Accounts - Staff", description = "Staff account management operations")
public class StaffAccountController {

    private final AccountService accountService;

    @GetMapping("/my-branch")
    @Operation(summary = "View branch accounts. Optional query param: status (ACTIVE/FROZEN/CLOSED)")
    public ResponseEntity<ApiResponse<List<AccountResponse>>> getBranchAccounts(
            @RequestParam(required = false) String status,
            @RequestHeader(value = "X-User-Id", defaultValue = "STAFF001") String userId,
            @RequestHeader(value = "X-Branch-Code", defaultValue = "BR001") String branchCode,
            @RequestHeader(value = "X-Role", defaultValue = "CSR") String role) {
        UserContext userContext = new UserContext(userId, userId, role, branchCode, null, null, null);
        List<AccountResponse> accounts = accountService.getBranchAccounts(userContext.getBranchCode(), status);
        return ResponseEntity.ok(ApiResponse.success("Branch accounts retrieved", accounts));
    }

    @GetMapping("/my-branch/summary")
    @Operation(summary = "View branch account summary/KPIs")
    public ResponseEntity<ApiResponse<BranchAccountSummary>> getBranchSummary(
            @RequestHeader(value = "X-Branch-Code", defaultValue = "BR001") String branchCode) {
        BranchAccountSummary summary = accountService.getBranchSummary(branchCode);
        return ResponseEntity.ok(ApiResponse.success("Branch summary retrieved", summary));
    }

    @PatchMapping("/{accountNo}/freeze")
    @Operation(summary = "Freeze a customer account")
    public ResponseEntity<ApiResponse<AccountResponse>> freezeAccount(
            @PathVariable String accountNo,
            @Valid @RequestBody FreezeAccountRequest request,
            @RequestHeader(value = "X-User-Id", defaultValue = "STAFF001") String userId,
            @RequestHeader(value = "X-Branch-Code", defaultValue = "BR001") String branchCode,
            @RequestHeader(value = "X-Role", defaultValue = "CSR") String role) {
        UserContext userContext = new UserContext(userId, userId, role, branchCode, null, null, null);
        AccountResponse response = accountService.freezeAccount(accountNo, request, userContext);
        return ResponseEntity.ok(ApiResponse.success("Account frozen successfully", response));
    }

    @PatchMapping("/{accountNo}/unfreeze")
    @Operation(summary = "Unfreeze a customer account")
    public ResponseEntity<ApiResponse<AccountResponse>> unfreezeAccount(
            @PathVariable String accountNo,
            @RequestHeader(value = "X-User-Id", defaultValue = "STAFF001") String userId,
            @RequestHeader(value = "X-Branch-Code", defaultValue = "BR002") String branchCode,
            @RequestHeader(value = "X-Role", defaultValue = "CSR") String role) {
        UserContext userContext = new UserContext(userId, userId, role, branchCode, null, null, null);
        AccountResponse response = accountService.unfreezeAccount(accountNo, userContext);
        return ResponseEntity.ok(ApiResponse.success("Account unfrozen successfully", response));
    }

    @PatchMapping("/{accountNo}/close")
    @Operation(summary = "Close a customer account. Balance must be zero.")
    public ResponseEntity<ApiResponse<AccountResponse>> closeAccount(
            @PathVariable String accountNo,
            @Valid @RequestBody CloseAccountRequest request,
            @RequestHeader(value = "X-User-Id", defaultValue = "MGR001") String userId,
            @RequestHeader(value = "X-Branch-Code", defaultValue = "BR001") String branchCode,
            @RequestHeader(value = "X-Role", defaultValue = "BRANCH_MANAGER") String role) {
        UserContext userContext = new UserContext(userId, userId, role, branchCode, null, null, null);
        AccountResponse response = accountService.closeAccount(accountNo, request, userContext);
        return ResponseEntity.ok(ApiResponse.success("Account closed successfully", response));
    }
}
