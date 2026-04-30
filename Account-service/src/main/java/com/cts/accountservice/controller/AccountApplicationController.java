package com.cts.accountservice.controller;

import com.cts.accountservice.dto.request.AccountApplicationRequest;
import com.cts.accountservice.dto.response.AccountApplicationResponse;
import com.cts.accountservice.dto.response.ApiResponse;
import com.cts.accountservice.security.UserContext;
import com.cts.accountservice.service.AccountApplicationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/account-applications")
@RequiredArgsConstructor
@Tag(name = "Account Applications - Customer", description = "Customer account application operations")
public class AccountApplicationController {

    private final AccountApplicationService applicationService;

    @PostMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Apply for a new bank account", description = "Customer submits application for Savings/Current account. KYC must be approved.")
    public ResponseEntity<ApiResponse<AccountApplicationResponse>> applyForAccount(
            @Valid @RequestBody AccountApplicationRequest request,
            @AuthenticationPrincipal UserContext userContext) {
        AccountApplicationResponse response = applicationService.applyForAccount(request, userContext);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Account application submitted successfully", response));
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "View my account applications", description = "Customer views all their account applications")
    public ResponseEntity<ApiResponse<List<AccountApplicationResponse>>> getMyApplications(
            @AuthenticationPrincipal UserContext userContext) {
        List<AccountApplicationResponse> applications = applicationService.getMyApplications(userContext);
        return ResponseEntity.ok(ApiResponse.success("Applications retrieved successfully", applications));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "View application by ID")
    public ResponseEntity<ApiResponse<AccountApplicationResponse>> getApplicationById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserContext userContext) {
        AccountApplicationResponse response = applicationService.getApplicationById(id, userContext);
        return ResponseEntity.ok(ApiResponse.success("Application retrieved successfully", response));
    }
}

