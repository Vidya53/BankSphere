package com.cts.accountservice.controller;

import com.cts.accountservice.dto.request.CloseAccountRequest;
import com.cts.accountservice.dto.request.FreezeAccountRequest;
import com.cts.accountservice.dto.response.AccountResponse;
import com.cts.accountservice.dto.response.ApiResponse;
import com.cts.accountservice.dto.response.BranchAccountSummary;
import com.cts.accountservice.dto.response.BranchAccountTypeBreakdown;
import com.cts.accountservice.context.UserContext;
import com.cts.accountservice.context.UserContextExtractor;
import com.cts.accountservice.exception.MissingGatewayHeaderException;
import com.cts.accountservice.service.AccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/staff/accounts")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('CSR','BRANCH_MANAGER','ADMIN')")
@Tag(name = "Accounts · Staff", description = "Staff-facing account management (branch-scoped reads plus freeze / unfreeze / close)")
public class StaffAccountController {

    private final AccountService accountService;
    private final UserContextExtractor userContextExtractor;

    @GetMapping("/my-branch")
    @Operation(
            summary = "List accounts in my branch",
            description = """
                    Returns accounts belonging to the authenticated staff member's branch (derived from `X-Branch-Code`). Optional `status` query filters by ACTIVE/FROZEN/CLOSED.

                    **Allowed roles:** CSR, BRANCH_MANAGER, ADMIN"""
    )
    public ResponseEntity<ApiResponse<List<AccountResponse>>> getBranchAccounts(
            @RequestParam(required = false) String status,
            HttpServletRequest request) {
        UserContext ctx = userContextExtractor.extract(request);
        String branchCode = requireBranchCode(ctx);
        List<AccountResponse> accounts = accountService.getBranchAccounts(branchCode, status);
        return ResponseEntity.ok(ApiResponse.success("Branch accounts retrieved", accounts));
    }

    @GetMapping("/my-branch/summary")
    @Operation(
            summary = "Branch account summary KPIs",
            description = """
                    Returns headline metrics (total accounts, active accounts, total deposits, etc.) for the authenticated staff member's branch.

                    **Allowed roles:** CSR, BRANCH_MANAGER, ADMIN"""
    )
    public ResponseEntity<ApiResponse<BranchAccountSummary>> getBranchSummary(HttpServletRequest request) {
        UserContext ctx = userContextExtractor.extract(request);
        BranchAccountSummary summary = accountService.getBranchSummary(requireBranchCode(ctx));
        return ResponseEntity.ok(ApiResponse.success("Branch summary retrieved", summary));
    }

    @GetMapping("/my-branch/breakdown")
    @Operation(
            summary = "Account-type breakdown for my branch",
            description = """
                    Returns counts of distinct customers and accounts per account type (SAVINGS / CURRENT / SALARY / …) for the staff member's branch.

                    **Allowed roles:** CSR, BRANCH_MANAGER, ADMIN"""
    )
    public ResponseEntity<ApiResponse<List<BranchAccountTypeBreakdown>>> getMyBranchBreakdown(
            HttpServletRequest request) {
        UserContext ctx = userContextExtractor.extract(request);
        List<BranchAccountTypeBreakdown> rows =
                accountService.getBranchAccountTypeBreakdown(requireBranchCode(ctx));
        return ResponseEntity.ok(ApiResponse.success("Branch account-type breakdown retrieved", rows));
    }

    // ── Admin: view ANY branch (not restricted to the caller's own branch) ────

    @GetMapping("/by-branch/{branchCode}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Admin: list accounts for any branch",
            description = """
                    Bypasses the caller's own branch and lists accounts for the supplied `branchCode`. Optional `status` filters by ACTIVE/FROZEN/CLOSED.

                    **Allowed roles:** ADMIN"""
    )
    public ResponseEntity<ApiResponse<List<AccountResponse>>> getAccountsForBranch(
            @PathVariable String branchCode,
            @RequestParam(required = false) String status) {
        List<AccountResponse> accounts = accountService.getBranchAccounts(branchCode, status);
        return ResponseEntity.ok(ApiResponse.success("Branch accounts retrieved", accounts));
    }

    @GetMapping("/by-branch/{branchCode}/summary")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Admin: branch summary KPIs for any branch",
            description = """
                    Returns the same KPI bundle as `/my-branch/summary`, but for any branch the admin chooses.

                    **Allowed roles:** ADMIN"""
    )
    public ResponseEntity<ApiResponse<BranchAccountSummary>> getSummaryForBranch(
            @PathVariable String branchCode) {
        BranchAccountSummary summary = accountService.getBranchSummary(branchCode);
        return ResponseEntity.ok(ApiResponse.success("Branch summary retrieved", summary));
    }

    @GetMapping("/by-branch/{branchCode}/breakdown")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Admin: account-type breakdown for any branch",
            description = """
                    Returns distinct-customer and account counts per account type for the supplied branch.

                    **Allowed roles:** ADMIN"""
    )
    public ResponseEntity<ApiResponse<List<BranchAccountTypeBreakdown>>> getBranchAccountTypeBreakdown(
            @PathVariable String branchCode) {
        List<BranchAccountTypeBreakdown> rows = accountService.getBranchAccountTypeBreakdown(branchCode);
        return ResponseEntity.ok(ApiResponse.success("Branch account-type breakdown retrieved", rows));
    }

    @PatchMapping("/{accountNo}/freeze")
    @PreAuthorize("hasRole('BRANCH_MANAGER')")
    @Operation(
            summary = "Freeze an active account",
            description = """
                    Transitions the account from `ACTIVE` to `FROZEN`, blocking all debits and transfers until it is unfrozen. A reason is required for the audit trail.

                    **Allowed roles:** BRANCH_MANAGER
                    **Side effects:** Emits audit events on `banking.audit.events` and a notification on `banking.notification.events`."""
    )
    public ResponseEntity<ApiResponse<AccountResponse>> freezeAccount(
            @PathVariable String accountNo,
            @Valid @RequestBody FreezeAccountRequest freezeRequest,
            HttpServletRequest request) {
        UserContext ctx = userContextExtractor.extract(request);
        AccountResponse response = accountService.freezeAccount(accountNo, freezeRequest, ctx);
        return ResponseEntity.ok(ApiResponse.success("Account frozen successfully", response));
    }

    @PatchMapping("/{accountNo}/unfreeze")
    @PreAuthorize("hasRole('BRANCH_MANAGER')")
    @Operation(
            summary = "Unfreeze a frozen account",
            description = """
                    Returns a `FROZEN` account to `ACTIVE` status, re-enabling transactions.

                    **Allowed roles:** BRANCH_MANAGER
                    **Side effects:** Emits audit events on `banking.audit.events` and a notification on `banking.notification.events`."""
    )
    public ResponseEntity<ApiResponse<AccountResponse>> unfreezeAccount(
            @PathVariable String accountNo,
            HttpServletRequest request) {
        UserContext ctx = userContextExtractor.extract(request);
        AccountResponse response = accountService.unfreezeAccount(accountNo, ctx);
        return ResponseEntity.ok(ApiResponse.success("Account unfrozen successfully", response));
    }

    @PatchMapping("/{accountNo}/close")
    @PreAuthorize("hasRole('BRANCH_MANAGER')")
    @Operation(
            summary = "Close an account",
            description = """
                    Transitions the account to terminal `CLOSED` status. Refuses to close while the balance is non-zero — funds must be withdrawn or transferred first.

                    **Allowed roles:** BRANCH_MANAGER
                    **Side effects:** Emits audit events on `banking.audit.events` and a notification on `banking.notification.events`."""
    )
    public ResponseEntity<ApiResponse<AccountResponse>> closeAccount(
            @PathVariable String accountNo,
            @Valid @RequestBody CloseAccountRequest closeRequest,
            HttpServletRequest request) {
        UserContext ctx = userContextExtractor.extract(request);
        AccountResponse response = accountService.closeAccount(accountNo, closeRequest, ctx);
        return ResponseEntity.ok(ApiResponse.success("Account closed successfully", response));
    }

    private String requireBranchCode(UserContext ctx) {
        if (ctx.getBranchCode() == null) {
            throw new MissingGatewayHeaderException("Required gateway header 'X-Branch-Code' is missing for staff operations.");
        }
        return ctx.getBranchCode();
    }
}
