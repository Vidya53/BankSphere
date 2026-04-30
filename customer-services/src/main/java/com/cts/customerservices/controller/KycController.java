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
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequiredArgsConstructor
@Tag(name = "KYC Management", description = "APIs for handling Know Your Customer (KYC) submissions, approvals, and rejections")
public class KycController {

    private final KycService service;

    /**
     * Helper method to wrap data into a consistent ApiResponse format.
     */
    private <T> ResponseEntity<ApiResponse<T>> buildResponse(T data, String message, HttpStatus status) {
        ApiResponse<T> response = ApiResponse.<T>builder()
                .status("SUCCESS")
                .message(message)
                .data(data)
                .timestamp(LocalDateTime.now())
                .build();
        return new ResponseEntity<>(response, status);
    }

    @PostMapping("/customers/{customerNo}/kyc")
    @Operation(summary = "Submit KYC documents", description = "Allows a customer to submit their identification documents for verification.")
    public ResponseEntity<ApiResponse<KycResponseDTO>> submitKyc(
            @PathVariable String customerNo,
            @Valid @RequestBody KycRequestDTO request
    ) {
        KycResponseDTO responseBody = service.submitKyc(customerNo, request);
        return buildResponse(responseBody, "KYC documents submitted successfully", HttpStatus.CREATED);
    }

    @PutMapping("/customers/{customerNo}/kyc/approve")
    @Operation(summary = "Approve KYC", description = "Marks the customer's KYC status as verified and sets the verification date.")
    public ResponseEntity<ApiResponse<Void>> approveKyc(@PathVariable String customerNo) {
        service.approveKyc(customerNo);
        return buildResponse(null, "KYC approved successfully", HttpStatus.OK);
    }

    @PutMapping("/customers/{customerNo}/kyc/reject")
    @Operation(summary = "Reject KYC", description = "Rejects the KYC submission and records the reason for rejection.")
    public ResponseEntity<ApiResponse<Void>> rejectKyc(
            @PathVariable String customerNo,
            @RequestParam String reason
    ) {
        service.rejectKyc(customerNo, reason);
        return buildResponse(null, "KYC rejected. Reason: " + reason, HttpStatus.OK);
    }

    @GetMapping("/customers/{customerNo}/kyc")
    @Operation(summary = "Get KYC status", description = "Retrieves the current KYC record and status for a specific customer.")
    public ResponseEntity<ApiResponse<KycResponseDTO>> getKyc(@PathVariable String customerNo) {
        KycResponseDTO responseBody = service.getKycStatus(customerNo);
        return buildResponse(responseBody, "KYC status retrieved successfully", HttpStatus.OK);
    }

    @GetMapping("/kyc/pending")
    @Operation(summary = "List pending KYC", description = "Retrieves a list of all KYC submissions that are currently awaiting verification.")
    public ResponseEntity<ApiResponse<List<KycResponseDTO>>> pending() {
        List<KycResponseDTO> responseBody = service.getPendingKyc();
        return buildResponse(responseBody, "Pending KYC list retrieved", HttpStatus.OK);
    }
}