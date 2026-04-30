package com.cts.accountservice.controller;

import com.cts.accountservice.dto.request.RejectRequest;
import com.cts.accountservice.dto.response.AccountApplicationResponse;
import com.cts.accountservice.dto.response.ApiResponse;
import com.cts.accountservice.security.UserContext;
import com.cts.accountservice.service.AccountApplicationService;
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
@RequestMapping("/api/v1/staff/account-applications")
@RequiredArgsConstructor
@Tag(name = "Account Applications - Staff", description = "Staff operations for account application review")
public class StaffAccountApplicationController {

    private final AccountApplicationService applicationService;

    @GetMapping("/pending")
    @PreAuthorize("hasAnyRole('CSR', 'BRANCH_MANAGER')")
    @Operation(summary = "View pending account applications for my branch")
    public ResponseEntity<ApiResponse<List<AccountApplicationResponse>>> getPendingApplications(
            @AuthenticationPrincipal UserContext userContext) {
        List<AccountApplicationResponse> applications = applicationService.getPendingApplicationsByBranch(userContext.getBranchCode());
        return ResponseEntity.ok(ApiResponse.success("Pending applications retrieved", applications));
    }

    @GetMapping("/all")
    @PreAuthorize("hasAnyRole('CSR', 'BRANCH_MANAGER')")
    @Operation(summary = "View all account applications for my branch")
    public ResponseEntity<ApiResponse<List<AccountApplicationResponse>>> getAllApplications(
            @AuthenticationPrincipal UserContext userContext) {
        List<AccountApplicationResponse> applications = applicationService.getAllApplicationsByBranch(userContext.getBranchCode());
        return ResponseEntity.ok(ApiResponse.success("All applications retrieved", applications));
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('CSR', 'BRANCH_MANAGER')")
    @Operation(summary = "Approve an account application", description = "Creates account on approval")
    public ResponseEntity<ApiResponse<AccountApplicationResponse>> approveApplication(
            @PathVariable Long id,
            @AuthenticationPrincipal UserContext userContext) {
        AccountApplicationResponse response = applicationService.approveApplication(id, userContext);
        return ResponseEntity.ok(ApiResponse.success("Application approved. Account created: " + response.getGeneratedAccountNo(), response));
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('CSR', 'BRANCH_MANAGER')")
    @Operation(summary = "Reject an account application")
    public ResponseEntity<ApiResponse<AccountApplicationResponse>> rejectApplication(
            @PathVariable Long id,
            @Valid @RequestBody RejectRequest request,
            @AuthenticationPrincipal UserContext userContext) {
        AccountApplicationResponse response = applicationService.rejectApplication(id, request, userContext);
        return ResponseEntity.ok(ApiResponse.success("Application rejected", response));
    }
}

