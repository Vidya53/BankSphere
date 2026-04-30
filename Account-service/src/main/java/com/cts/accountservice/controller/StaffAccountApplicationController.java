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
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/staff/account-applications")
@RequiredArgsConstructor
@Tag(name = "Account Applications - Staff", description = "Staff operations for account application review")
public class StaffAccountApplicationController {

    private final AccountApplicationService applicationService;

    @GetMapping("/pending")
    @Operation(summary = "View pending account applications for my branch")
    public ResponseEntity<ApiResponse<List<AccountApplicationResponse>>> getPendingApplications(
            @RequestHeader(value = "X-User-Id", defaultValue = "STAFF001") String userId,
            @RequestHeader(value = "X-Branch-Code", defaultValue = "BR001") String branchCode,
            @RequestHeader(value = "X-Role", defaultValue = "CSR") String role) {
        UserContext userContext = new UserContext(userId, userId, role, branchCode, null, null, null);
        List<AccountApplicationResponse> applications = applicationService.getPendingApplicationsByBranch(userContext.getBranchCode());
        return ResponseEntity.ok(ApiResponse.success("Pending applications retrieved", applications));
    }

    @GetMapping("/all")
    @Operation(summary = "View all account applications for my branch")
    public ResponseEntity<ApiResponse<List<AccountApplicationResponse>>> getAllApplications(
            @RequestHeader(value = "X-User-Id", defaultValue = "STAFF001") String userId,
            @RequestHeader(value = "X-Branch-Code", defaultValue = "BR001") String branchCode,
            @RequestHeader(value = "X-Role", defaultValue = "CSR") String role) {
        UserContext userContext = new UserContext(userId, userId, role, branchCode, null, null, null);
        List<AccountApplicationResponse> applications = applicationService.getAllApplicationsByBranch(userContext.getBranchCode());
        return ResponseEntity.ok(ApiResponse.success("All applications retrieved", applications));
    }

    @PostMapping("/{id}/approve")
    @Operation(summary = "Approve an account application — creates account on approval")
    public ResponseEntity<ApiResponse<AccountApplicationResponse>> approveApplication(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Id", defaultValue = "STAFF001") String userId,
            @RequestHeader(value = "X-Branch-Code", defaultValue = "BR001") String branchCode,
            @RequestHeader(value = "X-Role", defaultValue = "CSR") String role) {
        UserContext userContext = new UserContext(userId, userId, role, branchCode, null, null, null);
        AccountApplicationResponse response = applicationService.approveApplication(id, userContext);
        return ResponseEntity.ok(ApiResponse.success("Application approved. Account created: " + response.getGeneratedAccountNo(), response));
    }

    @PostMapping("/{id}/reject")
    @Operation(summary = "Reject an account application with reason")
    public ResponseEntity<ApiResponse<AccountApplicationResponse>> rejectApplication(
            @PathVariable Long id,
            @Valid @RequestBody RejectRequest request,
            @RequestHeader(value = "X-User-Id", defaultValue = "STAFF001") String userId,
            @RequestHeader(value = "X-Branch-Code", defaultValue = "BR001") String branchCode,
            @RequestHeader(value = "X-Role", defaultValue = "CSR") String role) {
        UserContext userContext = new UserContext(userId, userId, role, branchCode, null, null, null);
        AccountApplicationResponse response = applicationService.rejectApplication(id, request, userContext);
        return ResponseEntity.ok(ApiResponse.success("Application rejected", response));
    }
}
