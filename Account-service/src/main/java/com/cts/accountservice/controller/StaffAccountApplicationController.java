package com.cts.accountservice.controller;

import com.cts.accountservice.dto.request.RejectRequest;
import com.cts.accountservice.dto.response.AccountApplicationResponse;
import com.cts.accountservice.dto.response.ApiResponse;
import com.cts.accountservice.context.UserContext;
import com.cts.accountservice.context.UserContextExtractor;
import com.cts.accountservice.service.AccountApplicationService;
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
@RequestMapping("/api/v1/staff/account-applications")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('CSR','LOAN_OFFICER','BRANCH_MANAGER','ADMIN')")
@Tag(name = "Account Applications · Staff", description = "Branch-staff review queue for customer-submitted account applications")
public class StaffAccountApplicationController {

    private final AccountApplicationService applicationService;
    private final UserContextExtractor userContextExtractor;

    @GetMapping("/pending")
    @Operation(
            summary = "List pending applications in my branch",
            description = """
                    Returns applications still in `SUBMITTED` or `UNDER_REVIEW` status for the staff member's branch — the review queue.

                    **Allowed roles:** CSR, LOAN_OFFICER, BRANCH_MANAGER, ADMIN"""
    )
    public ResponseEntity<ApiResponse<List<AccountApplicationResponse>>> getPendingApplications(HttpServletRequest request) {
        UserContext ctx = userContextExtractor.extract(request);
        List<AccountApplicationResponse> applications = applicationService.getPendingApplicationsByBranch(ctx.getBranchCode());
        return ResponseEntity.ok(ApiResponse.success("Pending applications retrieved", applications));
    }

    @GetMapping("/all")
    @Operation(
            summary = "List all applications in my branch",
            description = """
                    Returns every application for the staff member's branch regardless of status (SUBMITTED, UNDER_REVIEW, APPROVED, REJECTED).

                    **Allowed roles:** CSR, LOAN_OFFICER, BRANCH_MANAGER, ADMIN"""
    )
    public ResponseEntity<ApiResponse<List<AccountApplicationResponse>>> getAllApplications(HttpServletRequest request) {
        UserContext ctx = userContextExtractor.extract(request);
        List<AccountApplicationResponse> applications = applicationService.getAllApplicationsByBranch(ctx.getBranchCode());
        return ResponseEntity.ok(ApiResponse.success("All applications retrieved", applications));
    }

    @PostMapping("/{id}/approve")
    @Operation(
            summary = "Approve an account application",
            description = """
                    Transitions the application to `APPROVED` and atomically creates a new `ACTIVE` account for the applicant. Best-effort calls `customer-service` `POST /api/v1/internal/customers/{userId}/activate` to auto-activate the customer.

                    **Allowed roles:** CSR, LOAN_OFFICER, BRANCH_MANAGER, ADMIN
                    **Side effects:** Creates an account; emits audit events on `banking.audit.events` and a notification on `banking.notification.events`."""
    )
    public ResponseEntity<ApiResponse<AccountApplicationResponse>> approveApplication(
            @PathVariable Long id,
            HttpServletRequest request) {
        UserContext ctx = userContextExtractor.extract(request);
        AccountApplicationResponse response = applicationService.approveApplication(id, ctx);
        return ResponseEntity.ok(ApiResponse.success(
                "Application approved. Account created: " + response.getGeneratedAccountNo(), response));
    }

    @PostMapping("/{id}/reject")
    @Operation(
            summary = "Reject an account application",
            description = """
                    Marks the application `REJECTED` with the supplied reason. No account is created.

                    **Allowed roles:** CSR, LOAN_OFFICER, BRANCH_MANAGER, ADMIN
                    **Side effects:** Emits audit events on `banking.audit.events` and a notification on `banking.notification.events`."""
    )
    public ResponseEntity<ApiResponse<AccountApplicationResponse>> rejectApplication(
            @PathVariable Long id,
            @Valid @RequestBody RejectRequest rejectRequest,
            HttpServletRequest request) {
        UserContext ctx = userContextExtractor.extract(request);
        AccountApplicationResponse response = applicationService.rejectApplication(id, rejectRequest, ctx);
        return ResponseEntity.ok(ApiResponse.success("Application rejected", response));
    }
}
