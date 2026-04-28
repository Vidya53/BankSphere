package com.cts.loanservice.service;

import com.cts.loanservice.dto.request.EmiPaymentRequest;
import com.cts.loanservice.dto.request.LoanApplyRequest;
import com.cts.loanservice.dto.request.LoanDecisionRequest;
import com.cts.loanservice.dto.response.EmiScheduleResponse;
import com.cts.loanservice.dto.response.LoanResponse;
import com.cts.loanservice.dto.response.LoanSummaryResponse;

public interface LoanService {

    LoanResponse applyLoan(LoanApplyRequest request);

    LoanResponse decideLoan(String loanId, LoanDecisionRequest request);

    LoanResponse disburse(String loanId);

    LoanResponse payEmi(String loanId, EmiPaymentRequest request);

    EmiScheduleResponse getSchedule(String loanId);

    LoanSummaryResponse getSummary(String customerId);

    Double getOutstanding(String customerId);
}