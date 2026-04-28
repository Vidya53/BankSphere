package com.cts.loanservice.service.impl;

import com.cts.loanservice.client.AccountClient;
import com.cts.loanservice.client.CustomerClient;
import com.cts.loanservice.dto.request.EmiPaymentRequest;
import com.cts.loanservice.dto.request.LoanApplyRequest;
import com.cts.loanservice.dto.request.LoanDecisionRequest;
import com.cts.loanservice.dto.response.EmiScheduleResponse;
import com.cts.loanservice.dto.response.LoanResponse;
import com.cts.loanservice.dto.response.LoanSummaryResponse;
import com.cts.loanservice.entity.Loan;
import com.cts.loanservice.entity.LoanStatus;
import com.cts.loanservice.exception.BusinessException;
import com.cts.loanservice.exception.ResourceNotFoundException;
import com.cts.loanservice.repository.LoanRepository;
import com.cts.loanservice.service.LoanService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LoanServiceImpl implements LoanService {

    private final LoanRepository repo;
    private final AccountClient accountClient;
    private final CustomerClient customerClient;

    public LoanResponse applyLoan(LoanApplyRequest req) {

        if (!customerClient.isEligible(req.getCustomerId())) {
            throw new BusinessException("Not eligible");
        }

        Loan loan = new Loan();
        loan.setCustomerId(req.getCustomerId());
        loan.setAmount(req.getAmount());
        loan.setRemainingAmount(req.getAmount());
        loan.setTenureMonths(req.getTenureMonths());
        loan.setStatus(LoanStatus.APPLIED);
        loan.setCreatedAt(LocalDateTime.now());

        return map(repo.save(loan));
    }

    public LoanResponse decideLoan(String id, LoanDecisionRequest req) {

        Loan loan = get(id);

        if (loan.getStatus() != LoanStatus.APPLIED) {
            throw new BusinessException("Already processed");
        }

        if ("APPROVED".equalsIgnoreCase(req.getStatus())) {
            loan.setStatus(LoanStatus.APPROVED);
            loan.setInterestRate(req.getInterestRate());

            double emi = calculateEmi(
                    loan.getAmount(),
                    loan.getInterestRate(),
                    loan.getTenureMonths()
            );

            loan.setEmiAmount(emi);
        } else {
            loan.setStatus(LoanStatus.REJECTED);
        }

        return map(repo.save(loan));
    }

    public LoanResponse disburse(String id) {

        Loan loan = get(id);

        if (loan.getStatus() != LoanStatus.APPROVED) {
            throw new BusinessException("Not approved");
        }

        accountClient.credit("ACCOUNT_ID", loan.getAmount());

        loan.setStatus(LoanStatus.DISBURSED);

        return map(repo.save(loan));
    }

    public LoanResponse payEmi(String id, EmiPaymentRequest req) {

        Loan loan = get(id);

        accountClient.debit(req.getAccountId(), req.getAmount());

        double remaining = loan.getRemainingAmount() - req.getAmount();
        loan.setRemainingAmount(remaining);

        if (remaining <= 0) {
            loan.setStatus(LoanStatus.CLOSED);
        }

        return map(repo.save(loan));
    }

    public EmiScheduleResponse getSchedule(String id) {

        Loan loan = get(id);

        List<Double> list = new ArrayList<>();

        for (int i = 0; i < loan.getTenureMonths(); i++) {
            list.add(loan.getEmiAmount());
        }

        return new EmiScheduleResponse(loan.getEmiAmount(), list);
    }

    public LoanSummaryResponse getSummary(String customerId) {

        List<Loan> loans = repo.findByCustomerId(customerId);

        long active = loans.stream().filter(l -> l.getStatus() == LoanStatus.DISBURSED).count();
        long closed = loans.stream().filter(l -> l.getStatus() == LoanStatus.CLOSED).count();

        return new LoanSummaryResponse(loans.size(), active, closed);
    }

    public Double getOutstanding(String customerId) {
        return repo.getOutstanding(customerId);
    }

    private Loan get(String id) {
        return repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Loan not found"));
    }

    private LoanResponse map(Loan loan) {
        return LoanResponse.builder()
                .loanId(loan.getLoanId())
                .customerId(loan.getCustomerId())
                .amount(loan.getAmount())
                .remainingAmount(loan.getRemainingAmount())
                .status(loan.getStatus().name())
                .build();
    }

    private double calculateEmi(double p, double r, int n) {
        double rate = r / (12 * 100);
        return (p * rate * Math.pow(1 + rate, n)) /
                (Math.pow(1 + rate, n) - 1);
    }
}