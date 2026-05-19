package com.cts.loanservice.controller;

import com.cts.loanservice.dto.request.*;
import com.cts.loanservice.dto.response.*;
import com.cts.loanservice.service.LoanService;
import com.cts.loanservice.util.ApiResponse;
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
@RequestMapping("/loans")
@RequiredArgsConstructor
@Tag(name = "Loans", description = "Loan management APIs")
public class LoanController {

    private final LoanService service;

    @PostMapping
    @PreAuthorize("hasAnyRole('CUSTOMER','LOAN_OFFICER','BRANCH_MANAGER','ADMIN')")
    @Operation(
            summary = "Apply for a new loan",
            description = """
                    Creates a new loan application in APPLIED state. Eligibility is precomputed (max EMI ≤ 50% of monthly income using a 10.5% indicative rate) but the application is still routed to a loan officer for explicit decisioning.

                    **Allowed roles:** CUSTOMER, LOAN_OFFICER, BRANCH_MANAGER, ADMIN
                    **Side effects:** Persists a new loan record with status=APPLIED."""
    )
    public ResponseEntity<ApiResponse<LoanResponse>> apply(@Valid @RequestBody LoanApplyRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(service.applyLoan(req)));
    }

    @PostMapping("/{id}/decision")
    @PreAuthorize("hasAnyRole('LOAN_OFFICER','BRANCH_MANAGER','ADMIN')")
    @Operation(
            summary = "Approve or reject a loan",
            description = """
                    Transitions a loan from APPLIED to either APPROVED or REJECTED based on the loan officer's decision and accompanying remarks. APPROVED loans become eligible for disbursement; REJECTED is terminal.

                    **Allowed roles:** LOAN_OFFICER, BRANCH_MANAGER, ADMIN
                    **Side effects:** Transitions APPLIED → APPROVED/REJECTED with decision metadata."""
    )
    public ResponseEntity<ApiResponse<LoanResponse>> decide(
            @PathVariable("id") Long id,
            @Valid @RequestBody LoanDecisionRequest req) {
        return ResponseEntity.ok(ApiResponse.success(service.decideLoan(id, req)));
    }

    @PostMapping("/{id}/disburse")
    @PreAuthorize("hasAnyRole('LOAN_OFFICER','BRANCH_MANAGER','ADMIN')")
    @Operation(
            summary = "Disburse an approved loan",
            description = """
                    Disburses an APPROVED loan, generating the amortisation schedule and crediting the disbursement amount to the customer's linked account.

                    **Allowed roles:** LOAN_OFFICER, BRANCH_MANAGER, ADMIN
                    **Side effects:** Transitions APPROVED → DISBURSED, generates EMI schedule, triggers account credit."""
    )
    public ResponseEntity<ApiResponse<LoanResponse>> disburse(@PathVariable("id") Long id) {
        return ResponseEntity.ok(ApiResponse.success(service.disburse(id)));
    }

    @PostMapping("/{id}/pay")
    @Operation(
            summary = "Pay an EMI installment",
            description = """
                    Records an EMI payment for a DISBURSED loan. Late payments incur a 2% penalty of the EMI amount. Funds are debited from the linked account via `POST /debit-with-pin` on account-service.

                    **Allowed roles:** Any authenticated user
                    **Side effects:** Calls account-service debit-with-pin; increments paid-EMI counter; closes loan when last EMI is paid."""
    )
    public ResponseEntity<ApiResponse<LoanResponse>> pay(
            @PathVariable("id") Long id,
            @Valid @RequestBody EmiPaymentRequest req) {
        return ResponseEntity.ok(ApiResponse.success(service.payEmi(id, req)));
    }

    @GetMapping("/{id}/schedule")
    @Operation(
            summary = "Get EMI schedule for a loan",
            description = """
                    Returns the full amortisation schedule for a loan, including principal/interest split, due dates, and paid/unpaid status for each installment.

                    **Allowed roles:** Any authenticated user"""
    )
    public ResponseEntity<ApiResponse<EmiScheduleResponse>> schedule(@PathVariable("id") Long id) {
        return ResponseEntity.ok(ApiResponse.success(service.getSchedule(id)));
    }

    @GetMapping("/summary/{customerId}")
    @Operation(
            summary = "Get loan summary for a customer",
            description = """
                    Returns a per-customer rollup: number of active loans, total outstanding principal, next EMI due, and aggregate interest paid to date.

                    **Allowed roles:** Any authenticated user"""
    )
    public ResponseEntity<ApiResponse<LoanSummaryResponse>> summary(@PathVariable("customerId") String customerId) {
        return ResponseEntity.ok(ApiResponse.success(service.getSummary(customerId)));
    }

    @PostMapping("/eligibility")
    @Operation(
            summary = "Check loan eligibility for a customer",
            description = """
                    Pre-qualification check that computes the maximum loan amount the applicant can service. Caps EMI at 50% of declared monthly income and uses a 10.5% indicative interest rate for the calculation.

                    **Allowed roles:** Any authenticated user"""
    )
    public ResponseEntity<ApiResponse<EligibilityResponse>> checkEligibility(@Valid @RequestBody EligibilityCheckRequest req) {
        return ResponseEntity.ok(ApiResponse.success(service.checkEligibility(req)));
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "Get loan by ID",
            description = """
                    Fetches full details of a single loan by its numeric loan ID, including current status, schedule progress, and decision metadata.

                    **Allowed roles:** Any authenticated user"""
    )
    public ResponseEntity<ApiResponse<LoanResponse>> getLoanById(@PathVariable("id") Long id) {
        return ResponseEntity.ok(ApiResponse.success(service.getLoanById(id)));
    }

    @GetMapping("/customer/{customerId}")
    @Operation(
            summary = "Get all loans for a customer",
            description = """
                    Returns every loan ever booked for the given customer ID across all lifecycle states (APPLIED, APPROVED, REJECTED, DISBURSED, CLOSED).

                    **Allowed roles:** Any authenticated user"""
    )
    public ResponseEntity<ApiResponse<List<LoanResponse>>> getByCustomer(@PathVariable("customerId") String customerId) {
        return ResponseEntity.ok(ApiResponse.success(service.getLoansByCustomer(customerId)));
    }

    @GetMapping("/customer/{customerId}/status/{status}")
    @Operation(
            summary = "Get loans by customer and status",
            description = """
                    Returns loans for the customer filtered to a specific lifecycle status (APPLIED, APPROVED, REJECTED, DISBURSED, or CLOSED).

                    **Allowed roles:** Any authenticated user"""
    )
    public ResponseEntity<ApiResponse<List<LoanResponse>>> getByCustomerAndStatus(
            @PathVariable("customerId") String customerId,
            @PathVariable("status") String status) {
        return ResponseEntity.ok(ApiResponse.success(service.getLoansByCustomerAndStatus(customerId, status)));
    }

    @PostMapping("/{id}/prepay")
    @Operation(
            summary = "Prepay or foreclose a loan",
            description = """
                    Accepts a part-prepayment or full foreclosure against a DISBURSED loan. Full foreclosure attracts a 2% fee on the remaining principal; funds are debited from the linked account via account-service `POST /debit-with-pin`.

                    **Allowed roles:** Any authenticated user
                    **Side effects:** Calls account-service debit-with-pin; recomputes schedule or closes loan."""
    )
    public ResponseEntity<ApiResponse<PrepaymentResponse>> prepay(
            @PathVariable("id") Long id,
            @Valid @RequestBody PrepaymentRequest req) {
        return ResponseEntity.ok(ApiResponse.success(service.prepay(id, req)));
    }

    @GetMapping("/{id}/payments")
    @Operation(
            summary = "Get payment history for a loan",
            description = """
                    Returns the chronological list of every EMI, prepayment, and foreclosure event recorded against the loan, including any late-payment penalties charged.

                    **Allowed roles:** Any authenticated user"""
    )
    public ResponseEntity<ApiResponse<List<PaymentHistoryResponse>>> paymentHistory(@PathVariable("id") Long id) {
        return ResponseEntity.ok(ApiResponse.success(service.getPaymentHistory(id)));
    }
}
