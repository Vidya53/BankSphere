package com.cts.customerservices.controller;

import com.cts.customerservices.dto.KycRequestDTO;
import com.cts.customerservices.dto.KycResponseDTO;
import com.cts.customerservices.payload.ApiResponse;
import com.cts.customerservices.service.KycService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Tag(name = "KYC", description = "Know-Your-Customer document submission, review, approval, and rejection")
public class KycController {

    private final KycService service;

    @PostMapping("/customers/{customerNo}/kyc")
    @PreAuthorize("hasAnyRole('CUSTOMER','CSR','BRANCH_MANAGER','ADMIN')")
    @Operation(
        summary = "Submit KYC documents for a customer",
        description = """
                Records the identification documents (type and number) for a customer and places the
                KYC record in SUBMITTED state, ready for staff review.

                **Allowed roles:** CUSTOMER, CSR, BRANCH_MANAGER, ADMIN
                **Side effects:** Persists a new KYC row; emits an audit event."""
    )
    public ResponseEntity<ApiResponse<KycResponseDTO>> submitKyc(
            @PathVariable String customerNo,
            @Valid @RequestBody KycRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(service.submitKyc(customerNo, request), "KYC documents submitted successfully"));
    }

    @PutMapping("/customers/{customerNo}/kyc/approve")
    @PreAuthorize("hasAnyRole('CSR','BRANCH_MANAGER','ADMIN')")
    @Operation(
        summary = "Approve a pending KYC submission",
        description = """
                Transitions the KYC record to APPROVED, sets the verification timestamp, and unlocks
                downstream actions such as account application approval and loan disbursement.

                **Allowed roles:** CSR, BRANCH_MANAGER, ADMIN
                **Side effects:** Updates KYC status to APPROVED; emits an audit event."""
    )
    public ResponseEntity<ApiResponse<Void>> approveKyc(@PathVariable String customerNo) {
        service.approveKyc(customerNo);
        return ResponseEntity.ok(ApiResponse.success("KYC approved successfully"));
    }

    @PutMapping("/customers/{customerNo}/kyc/reject")
    @PreAuthorize("hasAnyRole('CSR','BRANCH_MANAGER','ADMIN')")
    @Operation(
        summary = "Reject a pending KYC submission with a reason",
        description = """
                Transitions the KYC record to REJECTED and stores the rejection reason. The customer
                must resubmit before any account or loan flows can proceed.

                **Allowed roles:** CSR, BRANCH_MANAGER, ADMIN
                **Side effects:** Updates KYC status to REJECTED; emits an audit event."""
    )
    public ResponseEntity<ApiResponse<Void>> rejectKyc(
            @PathVariable String customerNo,
            @RequestParam String reason) {
        service.rejectKyc(customerNo, reason);
        return ResponseEntity.ok(ApiResponse.success("KYC rejected. Reason: " + reason));
    }

    @GetMapping("/customers/{customerNo}/kyc")
    @Operation(
        summary = "Get the current KYC record for a customer",
        description = """
                Returns the latest KYC submission and its review status for the given customer.

                **Allowed roles:** Any authenticated user"""
    )
    public ResponseEntity<ApiResponse<KycResponseDTO>> getKyc(@PathVariable String customerNo) {
        return ResponseEntity.ok(ApiResponse.success(service.getKycStatus(customerNo), "KYC status retrieved successfully"));
    }

    @GetMapping("/kyc/pending")
    @PreAuthorize("hasAnyRole('CSR','BRANCH_MANAGER','ADMIN')")
    @Operation(
        summary = "List KYC submissions awaiting verification",
        description = """
                Returns KYC records in SUBMITTED or UNDER_REVIEW state. Pass a branchCode query param
                to narrow the queue to one branch (used by branch-scoped CSR dashboards).

                **Allowed roles:** CSR, BRANCH_MANAGER, ADMIN"""
    )
    public ResponseEntity<ApiResponse<List<KycResponseDTO>>> pending(
            @RequestParam(required = false) String branchCode) {
        List<KycResponseDTO> list = (branchCode != null && !branchCode.isBlank())
                ? service.getPendingKycByBranch(branchCode)
                : service.getPendingKyc();
        return ResponseEntity.ok(ApiResponse.success(list, "Pending KYC list retrieved: " + list.size()));
    }
}