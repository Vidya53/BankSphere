package com.cts.accountservice.controller;

import com.cts.accountservice.dto.request.AccountApplicationRequest;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/account-applications")
@RequiredArgsConstructor
@PreAuthorize("hasRole('CUSTOMER')")
@Tag(name = "Account Applications · Customer", description = "Customer self-service for submitting and tracking new-account applications")
public class AccountApplicationController {

    private final AccountApplicationService applicationService;
    private final UserContextExtractor userContextExtractor;

    @PostMapping
    @Operation(
            summary = "Submit a new account application",
            description = """
                    Creates a new account application in `SUBMITTED` status. The application enters the branch review queue (SUBMITTED → APPROVED → ACTIVE account created, or REJECTED).

                    **Allowed roles:** CUSTOMER
                    **Side effects:** Emits an audit event on `banking.audit.events`."""
    )
    public ResponseEntity<ApiResponse<AccountApplicationResponse>> applyForAccount(
            @Valid @RequestBody AccountApplicationRequest request,
            HttpServletRequest httpRequest) {
        UserContext ctx = userContextExtractor.extract(httpRequest);
        AccountApplicationResponse response = applicationService.applyForAccount(request, ctx);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Account application submitted successfully", response));
    }

    @GetMapping("/me")
    @Operation(
            summary = "List my account applications",
            description = """
                    Returns every account application submitted by the authenticated customer, regardless of status.

                    **Allowed roles:** CUSTOMER"""
    )
    public ResponseEntity<ApiResponse<List<AccountApplicationResponse>>> getMyApplications(HttpServletRequest request) {
        UserContext ctx = userContextExtractor.extract(request);
        List<AccountApplicationResponse> applications = applicationService.getMyApplications(ctx);
        return ResponseEntity.ok(ApiResponse.success("Applications retrieved successfully", applications));
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "Get application by ID",
            description = """
                    Fetches a single application; the service enforces that the caller owns the application before returning it.

                    **Allowed roles:** CUSTOMER"""
    )
    public ResponseEntity<ApiResponse<AccountApplicationResponse>> getApplicationById(
            @PathVariable Long id,
            HttpServletRequest request) {
        UserContext ctx = userContextExtractor.extract(request);
        AccountApplicationResponse response = applicationService.getApplicationById(id, ctx);
        return ResponseEntity.ok(ApiResponse.success("Application retrieved successfully", response));
    }
}
