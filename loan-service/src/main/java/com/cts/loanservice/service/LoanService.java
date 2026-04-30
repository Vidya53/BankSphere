package com.cts.loanservice.service;

import com.cts.loanservice.dto.request.EligibilityCheckRequest;
import com.cts.loanservice.dto.request.EmiPaymentRequest;
import com.cts.loanservice.dto.request.LoanApplyRequest;
import com.cts.loanservice.dto.request.LoanDecisionRequest;
import com.cts.loanservice.dto.request.PrepaymentRequest;
import com.cts.loanservice.dto.response.*;

import java.util.List;

public interface LoanService {

    LoanResponse applyLoan(LoanApplyRequest request);

    LoanResponse decideLoan(Long loanId, LoanDecisionRequest request);

    LoanResponse disburse(Long loanId);

    LoanResponse payEmi(Long loanId, EmiPaymentRequest request);

    EmiScheduleResponse getSchedule(Long loanId);

    LoanSummaryResponse getSummary(String customerId);

    Double getOutstanding(String customerId);

    // ===== NEW FEATURES =====

    LoanResponse getLoanById(Long loanId);

    List<LoanResponse> getLoansByCustomer(String customerId);

    List<LoanResponse> getLoansByCustomerAndStatus(String customerId, String status);

    EligibilityResponse checkEligibility(EligibilityCheckRequest request);

    PrepaymentResponse prepay(Long loanId, PrepaymentRequest request);

    List<PaymentHistoryResponse> getPaymentHistory(Long loanId);
}