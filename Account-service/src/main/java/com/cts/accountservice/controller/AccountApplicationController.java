package com.cts.accountservice.controller;

import com.cts.accountservice.dto.request.AccountApplicationRequest;
import com.cts.accountservice.dto.response.AccountApplicationResponse;
import com.cts.accountservice.dto.response.ApiResponse;
import com.cts.accountservice.security.UserContext;
import com.cts.accountservice.service.AccountApplicationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/account-applications")
@RequiredArgsConstructor
@Tag(name = "Account Applications - Customer", description = "Customer account application operations")
public class AccountApplicationController {

    private final AccountApplicationService applicationService;

    @PostMapping
    @Operation(summary = "Apply for a new bank account")
    public ResponseEntity<ApiResponse<AccountApplicationResponse>> applyForAccount(
            @Valid @RequestBody AccountApplicationRequest request,
            @RequestHeader(value = "X-User-Id", defaultValue = "CUST001") String userId,
            @RequestHeader(value = "X-Branch-Code", defaultValue = "BR001") String branchCode,
            @RequestHeader(value = "X-Customer-Name", defaultValue = "Rajesh Kumar") String customerName,
            @RequestHeader(value = "X-Email", defaultValue = "rajesh.kumar@email.com") String email,
            @RequestHeader(value = "X-Phone", defaultValue = "9876543210") String phone,
            @RequestHeader(value = "X-Role", defaultValue = "CUSTOMER") String role) {
        UserContext userContext = new UserContext(userId, userId, role, branchCode, customerName, email, phone);
        AccountApplicationResponse response = applicationService.applyForAccount(request, userContext);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Account application submitted successfully", response));
    }

    @GetMapping("/me")
    @Operation(summary = "View my account applications")
    public ResponseEntity<ApiResponse<List<AccountApplicationResponse>>> getMyApplications(
            @RequestHeader(value = "X-User-Id", defaultValue = "CUST001") String userId,
            @RequestHeader(value = "X-Role", defaultValue = "CUSTOMER") String role,
            @RequestHeader(value = "X-Branch-Code", defaultValue = "BR001") String branchCode) {
        UserContext userContext = new UserContext(userId, userId, role, branchCode, null, null, null);
        List<AccountApplicationResponse> applications = applicationService.getMyApplications(userContext);
        return ResponseEntity.ok(ApiResponse.success("Applications retrieved successfully", applications));
    }

    @GetMapping("/{id}")
    @Operation(summary = "View application by ID")
    public ResponseEntity<ApiResponse<AccountApplicationResponse>> getApplicationById(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Id", defaultValue = "CUST001") String userId,
            @RequestHeader(value = "X-Role", defaultValue = "CUSTOMER") String role,
            @RequestHeader(value = "X-Branch-Code", defaultValue = "BR001") String branchCode) {
        UserContext userContext = new UserContext(userId, userId, role, branchCode, null, null, null);
        AccountApplicationResponse response = applicationService.getApplicationById(id, userContext);
        return ResponseEntity.ok(ApiResponse.success("Application retrieved successfully", response));
    }
}
