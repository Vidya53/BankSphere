package com.cts.loanservice.controller;

import com.cts.loanservice.dto.request.*;
import com.cts.loanservice.dto.response.*;
import com.cts.loanservice.service.LoanService;
import com.cts.loanservice.util.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/loans")
@RequiredArgsConstructor
@Tag(name = "Loans", description = "Loan management APIs")
public class LoanController {

    private final LoanService service;

    @PostMapping
    @Operation(summary = "Apply for a new loan")
    public ResponseEntity<ApiResponse<LoanResponse>> apply(@Valid @RequestBody LoanApplyRequest req) {
        return ResponseEntity.ok(ApiResponse.success(service.applyLoan(req)));
    }

    @PostMapping("/{id}/decision")
    @Operation(summary = "Approve or reject a loan")
    public ResponseEntity<ApiResponse<LoanResponse>> decide(@PathVariable("id") Long id,
                                                            @Valid @RequestBody LoanDecisionRequest req) {
        return ResponseEntity.ok(ApiResponse.success(service.decideLoan(id, req)));
    }

    @PostMapping("/{id}/disburse")
    @Operation(summary = "Disburse an approved loan")
    public ResponseEntity<ApiResponse<LoanResponse>> disburse(@PathVariable("id") Long id) {
        return ResponseEntity.ok(ApiResponse.success(service.disburse(id)));
    }

    @PostMapping("/{id}/pay")
    @Operation(summary = "Pay an EMI installment")
    public ResponseEntity<ApiResponse<LoanResponse>> pay(@PathVariable("id") Long id,
                                                         @Valid @RequestBody EmiPaymentRequest req) {
        return ResponseEntity.ok(ApiResponse.success(service.payEmi(id, req)));
    }

    @GetMapping("/{id}/schedule")
    @Operation(summary = "Get EMI schedule for a loan")
    public ResponseEntity<ApiResponse<EmiScheduleResponse>> schedule(@PathVariable("id") Long id) {
        return ResponseEntity.ok(ApiResponse.success(service.getSchedule(id)));
    }

    @GetMapping("/summary/{customerId}")
    @Operation(summary = "Get loan summary for a customer")
    public ResponseEntity<ApiResponse<LoanSummaryResponse>> summary(@PathVariable("customerId") String customerId) {
        return ResponseEntity.ok(ApiResponse.success(service.getSummary(customerId)));
    }

    // ===== NEW ENDPOINTS =====

    @PostMapping("/eligibility")
    @Operation(summary = "Check loan eligibility for a customer")
    public ResponseEntity<ApiResponse<EligibilityResponse>> checkEligibility(@Valid @RequestBody EligibilityCheckRequest req) {
        return ResponseEntity.ok(ApiResponse.success(service.checkEligibility(req)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get loan by ID")
    public ResponseEntity<ApiResponse<LoanResponse>> getLoanById(@PathVariable("id") Long id) {
        return ResponseEntity.ok(ApiResponse.success(service.getLoanById(id)));
    }

    @GetMapping("/customer/{customerId}")
    @Operation(summary = "Get all loans for a customer")
    public ResponseEntity<ApiResponse<List<LoanResponse>>> getByCustomer(@PathVariable("customerId") String customerId) {
        return ResponseEntity.ok(ApiResponse.success(service.getLoansByCustomer(customerId)));
    }

    @GetMapping("/customer/{customerId}/status/{status}")
    @Operation(summary = "Get loans by customer and status")
    public ResponseEntity<ApiResponse<List<LoanResponse>>> getByCustomerAndStatus(
            @PathVariable("customerId") String customerId,
            @PathVariable("status") String status) {
        return ResponseEntity.ok(ApiResponse.success(service.getLoansByCustomerAndStatus(customerId, status)));
    }

    @PostMapping("/{id}/prepay")
    @Operation(summary = "Prepay or foreclose a loan")
    public ResponseEntity<ApiResponse<PrepaymentResponse>> prepay(@PathVariable("id") Long id,
                                                                   @Valid @RequestBody PrepaymentRequest req) {
        return ResponseEntity.ok(ApiResponse.success(service.prepay(id, req)));
    }

    @GetMapping("/{id}/payments")
    @Operation(summary = "Get payment history for a loan")
    public ResponseEntity<ApiResponse<List<PaymentHistoryResponse>>> paymentHistory(@PathVariable("id") Long id) {
        return ResponseEntity.ok(ApiResponse.success(service.getPaymentHistory(id)));
    }
}