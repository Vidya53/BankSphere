package com.cts.loanservice.service.impl;

import com.cts.loanservice.client.AccountClient;
import com.cts.loanservice.client.CustomerClient;
import com.cts.loanservice.dto.request.*;
import com.cts.loanservice.dto.response.*;
import com.cts.loanservice.entity.EmiPayment;
import com.cts.loanservice.entity.Loan;
import com.cts.loanservice.entity.LoanStatus;
import com.cts.loanservice.entity.LoanType;
import com.cts.loanservice.exception.BusinessException;
import com.cts.loanservice.exception.ResourceNotFoundException;
import com.cts.loanservice.repository.EmiPaymentRepository;
import com.cts.loanservice.repository.LoanRepository;
import com.cts.loanservice.service.LoanService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LoanServiceImpl implements LoanService {

    private final LoanRepository repo;
    private final EmiPaymentRepository emiPaymentRepo;
    private final AccountClient accountClient;
    private final CustomerClient customerClient;

    // Configurable constants
    private static final double MAX_EMI_TO_INCOME_RATIO = 0.50;   // 50% of income
    private static final double FORECLOSURE_CHARGE_RATE = 0.02;    // 2%
    private static final double LATE_PENALTY_RATE = 0.02;          // 2% of EMI
    private static final double DEFAULT_INTEREST_RATE = 10.5;      // for eligibility calc

    // ======================== EXISTING FEATURES ========================

    @Override
    @Transactional
    public LoanResponse applyLoan(LoanApplyRequest req) {

        if (!customerClient.isEligible(req.getCustomerId())) {
            throw new BusinessException("Customer is not eligible for a loan");
        }

        // Validate loan type
        LoanType loanType;
        try {
            loanType = LoanType.valueOf(req.getLoanType().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException("Invalid loan type: " + req.getLoanType()
                    + ". Allowed values: HOME, PERSONAL, CAR, EDUCATION, BUSINESS");
        }

        Loan loan = new Loan();
        loan.setCustomerId(req.getCustomerId());
        loan.setAccountId(req.getAccountId());
        loan.setLoanType(loanType);
        loan.setAmount(req.getAmount());
        loan.setRemainingAmount(req.getAmount());
        loan.setTenureMonths(req.getTenureMonths());
        loan.setStatus(LoanStatus.APPLIED);

        return map(repo.save(loan));
    }

    @Override
    @Transactional
    public LoanResponse decideLoan(Long id, LoanDecisionRequest req) {

        Loan loan = get(id);

        if (loan.getStatus() != LoanStatus.APPLIED) {
            throw new BusinessException("Loan has already been processed");
        }

        if ("APPROVED".equalsIgnoreCase(req.getStatus())) {
            if (req.getInterestRate() == null) {
                throw new BusinessException("Interest rate is required for approval");
            }
            loan.setStatus(LoanStatus.APPROVED);
            loan.setInterestRate(req.getInterestRate());

            double emi = calculateEmi(loan.getAmount(), loan.getInterestRate(), loan.getTenureMonths());
            loan.setEmiAmount(round(emi));
            loan.setRemarks("Loan approved with " + req.getInterestRate() + "% interest rate");
        } else {
            loan.setStatus(LoanStatus.REJECTED);
            loan.setRemarks("Loan application rejected");
        }

        return map(repo.save(loan));
    }

    @Override
    @Transactional
    public LoanResponse disburse(Long id) {

        Loan loan = get(id);

        if (loan.getStatus() != LoanStatus.APPROVED) {
            throw new BusinessException("Only approved loans can be disbursed");
        }

        accountClient.credit(loan.getAccountId(), loan.getAmount());

        loan.setStatus(LoanStatus.DISBURSED);
        loan.setDisbursedAt(LocalDateTime.now());
        loan.setNextDueDate(LocalDate.now().plusMonths(1));
        loan.setRemarks("Loan disbursed to account " + loan.getAccountId());

        return map(repo.save(loan));
    }

    @Override
    @Transactional
    public LoanResponse payEmi(Long id, EmiPaymentRequest req) {

        Loan loan = get(id);

        if (loan.getStatus() != LoanStatus.DISBURSED) {
            throw new BusinessException("Loan is not in disbursed state. Current status: " + loan.getStatus());
        }

        if (req.getAmount() > loan.getRemainingAmount()) {
            throw new BusinessException("Payment amount ₹" + req.getAmount()
                    + " exceeds remaining balance ₹" + loan.getRemainingAmount());
        }

        // Check for late payment penalty
        double penalty = 0.0;
        boolean isLate = false;
        if (loan.getNextDueDate() != null && LocalDate.now().isAfter(loan.getNextDueDate())) {
            penalty = round(loan.getEmiAmount() * LATE_PENALTY_RATE);
            isLate = true;
        }

        double totalDeduction = req.getAmount() + penalty;
        accountClient.debit(req.getAccountId(), totalDeduction);

        // Calculate interest & principal components
        double monthlyRate = loan.getInterestRate() / (12 * 100);
        double interestComponent = round(loan.getRemainingAmount() * monthlyRate);
        double principalComponent = round(req.getAmount() - interestComponent);

        double remaining = Math.max(0, round(loan.getRemainingAmount() - req.getAmount()));
        loan.setRemainingAmount(remaining);
        loan.setEmiPaidCount(loan.getEmiPaidCount() + 1);

        // Record payment history
        EmiPayment payment = new EmiPayment();
        payment.setLoan(loan);
        payment.setAmountPaid(req.getAmount());
        payment.setPrincipalComponent(Math.max(0, principalComponent));
        payment.setInterestComponent(interestComponent);
        payment.setPenaltyAmount(penalty);
        payment.setBalanceAfterPayment(remaining);
        payment.setDueDate(loan.getNextDueDate());
        payment.setPaidDate(LocalDate.now());
        payment.setLate(isLate);
        payment.setTransactionRef("TXN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        emiPaymentRepo.save(payment);

        if (remaining <= 0) {
            loan.setStatus(LoanStatus.CLOSED);
            loan.setNextDueDate(null);
            loan.setRemarks("Loan fully paid and closed");
        } else {
            loan.setNextDueDate(loan.getNextDueDate() != null
                    ? loan.getNextDueDate().plusMonths(1)
                    : LocalDate.now().plusMonths(1));
        }

        return map(repo.save(loan));
    }

    @Override
    public EmiScheduleResponse getSchedule(Long id) {

        Loan loan = get(id);

        if (loan.getEmiAmount() == null || loan.getInterestRate() == null) {
            throw new BusinessException("Loan is not yet approved. EMI schedule unavailable.");
        }

        double monthlyRate = loan.getInterestRate() / (12 * 100);
        double balance = loan.getAmount();
        double emi = loan.getEmiAmount();

        List<EmiScheduleResponse.EmiDetail> schedule = new ArrayList<>();

        for (int i = 1; i <= loan.getTenureMonths(); i++) {
            double interest = round(balance * monthlyRate);
            double principal = round(emi - interest);
            balance = Math.max(0, round(balance - principal));

            schedule.add(EmiScheduleResponse.EmiDetail.builder()
                    .month(i)
                    .emi(emi)
                    .principal(principal)
                    .interest(interest)
                    .balance(balance)
                    .build());
        }

        return new EmiScheduleResponse(emi, loan.getTenureMonths(), schedule);
    }

    @Override
    public LoanSummaryResponse getSummary(String customerId) {

        List<Loan> loans = repo.findByCustomerId(customerId);

        long active = loans.stream().filter(l -> l.getStatus() == LoanStatus.DISBURSED).count();
        long closed = loans.stream().filter(l -> l.getStatus() == LoanStatus.CLOSED).count();
        Double outstanding = repo.getOutstanding(customerId);

        return new LoanSummaryResponse(loans.size(), active, closed,
                outstanding != null ? outstanding : 0.0);
    }

    @Override
    public Double getOutstanding(String customerId) {
        Double val = repo.getOutstanding(customerId);
        return val != null ? val : 0.0;
    }

    // ======================== NEW FEATURES ========================

    @Override
    public LoanResponse getLoanById(Long loanId) {
        return map(get(loanId));
    }

    @Override
    public List<LoanResponse> getLoansByCustomer(String customerId) {
        return repo.findByCustomerId(customerId).stream()
                .map(this::map)
                .collect(Collectors.toList());
    }

    @Override
    public List<LoanResponse> getLoansByCustomerAndStatus(String customerId, String status) {
        LoanStatus loanStatus;
        try {
            loanStatus = LoanStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException("Invalid loan status: " + status
                    + ". Allowed values: APPLIED, APPROVED, REJECTED, DISBURSED, CLOSED");
        }

        return repo.findByCustomerIdAndStatus(customerId, loanStatus).stream()
                .map(this::map)
                .collect(Collectors.toList());
    }

    @Override
    public EligibilityResponse checkEligibility(EligibilityCheckRequest req) {

        if (!customerClient.isEligible(req.getCustomerId())) {
            return EligibilityResponse.builder()
                    .eligible(false)
                    .requestedAmount(req.getRequestedAmount())
                    .monthlyIncome(req.getMonthlyIncome())
                    .reason("Customer is not eligible as per KYC/credit check")
                    .build();
        }

        // Max EMI the customer can afford (50% of monthly income)
        double maxAllowedEmi = round(req.getMonthlyIncome() * MAX_EMI_TO_INCOME_RATIO);

        // Deduct existing active EMIs
        Double existingEmi = repo.getTotalActiveEmi(req.getCustomerId());
        double currentEmi = existingEmi != null ? existingEmi : 0.0;
        double availableEmiCapacity = maxAllowedEmi - currentEmi;

        if (availableEmiCapacity <= 0) {
            return EligibilityResponse.builder()
                    .eligible(false)
                    .requestedAmount(req.getRequestedAmount())
                    .monthlyIncome(req.getMonthlyIncome())
                    .maxAllowedEmi(maxAllowedEmi)
                    .existingOutstanding(repo.getOutstanding(req.getCustomerId()))
                    .reason("Existing EMI obligations exceed the allowed limit. Current active EMI: ₹" + currentEmi)
                    .build();
        }

        // Calculate max eligible loan amount based on available EMI capacity
        double rate = DEFAULT_INTEREST_RATE / (12 * 100);
        int tenure = req.getRequestedTenureMonths();
        double maxLoanAmount = round(
                availableEmiCapacity * (Math.pow(1 + rate, tenure) - 1) / (rate * Math.pow(1 + rate, tenure))
        );

        Double outstanding = repo.getOutstanding(req.getCustomerId());

        boolean eligible = req.getRequestedAmount() <= maxLoanAmount;

        return EligibilityResponse.builder()
                .eligible(eligible)
                .maxEligibleAmount(maxLoanAmount)
                .requestedAmount(req.getRequestedAmount())
                .existingOutstanding(outstanding != null ? outstanding : 0.0)
                .monthlyIncome(req.getMonthlyIncome())
                .maxAllowedEmi(maxAllowedEmi)
                .reason(eligible
                        ? "You are eligible! Max loan amount: ₹" + maxLoanAmount
                        : "Requested amount ₹" + req.getRequestedAmount()
                        + " exceeds max eligible ₹" + maxLoanAmount)
                .build();
    }

    @Override
    @Transactional
    public PrepaymentResponse prepay(Long loanId, PrepaymentRequest req) {

        Loan loan = get(loanId);

        if (loan.getStatus() != LoanStatus.DISBURSED) {
            throw new BusinessException("Prepayment is only allowed for disbursed loans");
        }

        double foreclosureCharge = 0.0;
        double prepayAmount;

        if (req.isFullForeclosure()) {
            // Full foreclosure: pay remaining + 2% charge
            foreclosureCharge = round(loan.getRemainingAmount() * FORECLOSURE_CHARGE_RATE);
            prepayAmount = loan.getRemainingAmount();
        } else {
            if (req.getAmount() > loan.getRemainingAmount()) {
                throw new BusinessException("Prepayment amount ₹" + req.getAmount()
                        + " exceeds remaining balance ₹" + loan.getRemainingAmount());
            }
            prepayAmount = req.getAmount();
        }

        double totalDeducted = round(prepayAmount + foreclosureCharge);
        accountClient.debit(req.getAccountId(), totalDeducted);

        double remaining = Math.max(0, round(loan.getRemainingAmount() - prepayAmount));
        loan.setRemainingAmount(remaining);

        // Record payment
        EmiPayment payment = new EmiPayment();
        payment.setLoan(loan);
        payment.setAmountPaid(prepayAmount);
        payment.setPrincipalComponent(prepayAmount);
        payment.setInterestComponent(0.0);
        payment.setPenaltyAmount(foreclosureCharge);
        payment.setBalanceAfterPayment(remaining);
        payment.setPaidDate(LocalDate.now());
        payment.setLate(false);
        payment.setTransactionRef("PRE-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        emiPaymentRepo.save(payment);

        String message;
        if (remaining <= 0) {
            loan.setStatus(LoanStatus.CLOSED);
            loan.setNextDueDate(null);
            loan.setRemarks("Loan foreclosed");
            message = "Loan fully foreclosed successfully";
        } else {
            // Recalculate EMI for remaining tenure
            int emisPaid = loan.getEmiPaidCount();
            int remainingMonths = loan.getTenureMonths() - emisPaid;
            if (remainingMonths > 0) {
                double newEmi = round(calculateEmi(remaining, loan.getInterestRate(), remainingMonths));
                loan.setEmiAmount(newEmi);
            }
            loan.setRemarks("Partial prepayment of ₹" + prepayAmount + ". EMI recalculated.");
            message = "Partial prepayment successful. EMI recalculated for remaining tenure.";
        }

        repo.save(loan);

        return PrepaymentResponse.builder()
                .loanId(loan.getLoanId())
                .prepaidAmount(prepayAmount)
                .foreclosureCharge(foreclosureCharge)
                .totalDeducted(totalDeducted)
                .remainingBalance(remaining)
                .status(loan.getStatus().name())
                .message(message)
                .build();
    }

    @Override
    public List<PaymentHistoryResponse> getPaymentHistory(Long loanId) {
        // Validate loan exists
        get(loanId);

        return emiPaymentRepo.findByLoanLoanIdOrderByPaidDateDesc(loanId).stream()
                .map(p -> PaymentHistoryResponse.builder()
                        .paymentId(p.getPaymentId())
                        .loanId(p.getLoan().getLoanId())
                        .amountPaid(p.getAmountPaid())
                        .principalComponent(p.getPrincipalComponent())
                        .interestComponent(p.getInterestComponent())
                        .penaltyAmount(p.getPenaltyAmount())
                        .balanceAfterPayment(p.getBalanceAfterPayment())
                        .dueDate(p.getDueDate())
                        .paidDate(p.getPaidDate())
                        .late(p.isLate())
                        .transactionRef(p.getTransactionRef())
                        .createdAt(p.getCreatedAt())
                        .build())
                .collect(Collectors.toList());
    }

    // ======================== HELPER METHODS ========================

    private Loan get(Long id) {
        return repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Loan not found with ID: " + id));
    }

    private LoanResponse map(Loan loan) {
        int emiPaid = loan.getEmiPaidCount() != null ? loan.getEmiPaidCount() : 0;
        int emisRemaining = loan.getTenureMonths() != null ? loan.getTenureMonths() - emiPaid : 0;

        return LoanResponse.builder()
                .loanId(loan.getLoanId())
                .customerId(loan.getCustomerId())
                .loanType(loan.getLoanType() != null ? loan.getLoanType().name() : null)
                .amount(loan.getAmount())
                .interestRate(loan.getInterestRate())
                .tenureMonths(loan.getTenureMonths())
                .emiAmount(loan.getEmiAmount())
                .remainingAmount(loan.getRemainingAmount())
                .emiPaidCount(emiPaid)
                .emisRemaining(Math.max(0, emisRemaining))
                .status(loan.getStatus().name())
                .nextDueDate(loan.getNextDueDate())
                .disbursedAt(loan.getDisbursedAt())
                .remarks(loan.getRemarks())
                .createdAt(loan.getCreatedAt())
                .build();
    }

    private double calculateEmi(double p, double r, int n) {
        double rate = r / (12 * 100);
        return (p * rate * Math.pow(1 + rate, n)) /
                (Math.pow(1 + rate, n) - 1);
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}

