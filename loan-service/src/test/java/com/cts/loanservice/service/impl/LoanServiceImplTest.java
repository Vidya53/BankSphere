package com.cts.loanservice.service.impl;

import com.cts.loanservice.client.AccountClient;
import com.cts.loanservice.client.CustomerClient;
import com.cts.loanservice.client.TransactionServiceClient;
import com.cts.loanservice.client.dto.AccountActiveResponse;
import com.cts.loanservice.client.dto.CustomerApiResponse;
import com.cts.loanservice.dto.request.EligibilityCheckRequest;
import com.cts.loanservice.dto.request.EmiPaymentRequest;
import com.cts.loanservice.dto.request.LoanApplyRequest;
import com.cts.loanservice.dto.request.LoanDecisionRequest;
import com.cts.loanservice.dto.request.PrepaymentRequest;
import com.cts.loanservice.dto.response.EligibilityResponse;
import com.cts.loanservice.dto.response.EmiScheduleResponse;
import com.cts.loanservice.dto.response.LoanResponse;
import com.cts.loanservice.dto.response.PrepaymentResponse;
import com.cts.loanservice.entity.EmiPayment;
import com.cts.loanservice.entity.Loan;
import com.cts.loanservice.entity.LoanStatus;
import com.cts.loanservice.entity.LoanType;
import com.cts.loanservice.exception.BusinessException;
import com.cts.loanservice.exception.ResourceNotFoundException;
import com.cts.loanservice.repository.EmiPaymentRepository;
import com.cts.loanservice.repository.LoanRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pure JUnit 5 + Mockito test for {@link LoanServiceImpl}.
 *
 * No Spring context, no database, no Feign clients — every collaborator is mocked.
 * Each test seeds only the stubs it needs (LENIENT strictness avoids
 * UnnecessaryStubbingException when failure branches short-circuit early).
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("LoanServiceImpl — business logic")
class LoanServiceImplTest {

    @Mock private LoanRepository repo;
    @Mock private EmiPaymentRepository emiPaymentRepo;
    @Mock private AccountClient accountClient;
    @Mock private CustomerClient customerClient;
    @Mock private TransactionServiceClient transactionServiceClient;

    @InjectMocks private LoanServiceImpl service;

    private CustomerApiResponse activeCustomer;
    private CustomerApiResponse inactiveCustomer;
    private AccountActiveResponse activeAccount;
    private AccountActiveResponse inactiveAccount;

    @BeforeEach
    void setup() {
        activeCustomer = new CustomerApiResponse(new CustomerApiResponse.CustomerData("ACTIVE"));
        inactiveCustomer = new CustomerApiResponse(new CustomerApiResponse.CustomerData("INACTIVE"));
        activeAccount = new AccountActiveResponse(Boolean.TRUE);
        inactiveAccount = new AccountActiveResponse(Boolean.FALSE);
    }

    // ────────────────────────────────────────────────────────────────────────
    //  applyLoan
    // ────────────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("applyLoan(...)")
    class ApplyLoan {

        private LoanApplyRequest req() {
            return new LoanApplyRequest("CUST1", "ACC1", "HOME",
                    500_000.0, 60, 80_000.0);
        }

        @Test
        @DisplayName("happy path — persists loan in APPLIED status")
        void happyPath() {
            LoanApplyRequest req = req();
            when(customerClient.getCustomerDetails("CUST1")).thenReturn(activeCustomer);
            when(accountClient.isAccountActive("ACC1")).thenReturn(activeAccount);
            when(repo.save(any(Loan.class))).thenAnswer(inv -> {
                Loan l = inv.getArgument(0);
                l.setLoanId(1L);
                return l;
            });

            LoanResponse resp = service.applyLoan(req);

            assertThat(resp.getStatus()).isEqualTo("APPLIED");
            assertThat(resp.getLoanType()).isEqualTo("HOME");
            assertThat(resp.getAmount()).isEqualTo(500_000.0);

            ArgumentCaptor<Loan> captor = ArgumentCaptor.forClass(Loan.class);
            verify(repo).save(captor.capture());
            Loan saved = captor.getValue();
            assertThat(saved.getCustomerId()).isEqualTo("CUST1");
            assertThat(saved.getAccountId()).isEqualTo("ACC1");
            assertThat(saved.getRemainingAmount()).isEqualTo(500_000.0);
            assertThat(saved.getStatus()).isEqualTo(LoanStatus.APPLIED);
        }

        @Test
        @DisplayName("throws BusinessException when customer not ACTIVE")
        void customerNotActive() {
            LoanApplyRequest req = req();
            when(customerClient.getCustomerDetails("CUST1")).thenReturn(inactiveCustomer);

            assertThatThrownBy(() -> service.applyLoan(req))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("not active");

            verify(repo, never()).save(any());
        }

        @Test
        @DisplayName("throws BusinessException when customer payload is null")
        void customerPayloadNull() {
            LoanApplyRequest req = req();
            when(customerClient.getCustomerDetails("CUST1")).thenReturn(null);

            assertThatThrownBy(() -> service.applyLoan(req))
                    .isInstanceOf(BusinessException.class);

            verify(repo, never()).save(any());
        }

        @Test
        @DisplayName("throws BusinessException when account not active")
        void accountNotActive() {
            LoanApplyRequest req = req();
            when(customerClient.getCustomerDetails("CUST1")).thenReturn(activeCustomer);
            when(accountClient.isAccountActive("ACC1")).thenReturn(inactiveAccount);

            assertThatThrownBy(() -> service.applyLoan(req))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("not active");

            verify(repo, never()).save(any());
        }

        @Test
        @DisplayName("throws BusinessException when loanType is invalid")
        void invalidLoanType() {
            LoanApplyRequest req = new LoanApplyRequest(
                    "CUST1", "ACC1", "SPACE_LOAN", 100_000.0, 12, 50_000.0);
            when(customerClient.getCustomerDetails("CUST1")).thenReturn(activeCustomer);
            when(accountClient.isAccountActive("ACC1")).thenReturn(activeAccount);

            assertThatThrownBy(() -> service.applyLoan(req))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Invalid loan type");

            verify(repo, never()).save(any());
        }
    }

    // ────────────────────────────────────────────────────────────────────────
    //  decideLoan
    // ────────────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("decideLoan(...)")
    class DecideLoan {

        private Loan appliedLoan() {
            Loan l = new Loan();
            l.setLoanId(1L);
            l.setCustomerId("CUST1");
            l.setAccountId("ACC1");
            l.setLoanType(LoanType.HOME);
            l.setAmount(500_000.0);
            l.setRemainingAmount(500_000.0);
            l.setTenureMonths(60);
            l.setStatus(LoanStatus.APPLIED);
            l.setEmiPaidCount(0);
            return l;
        }

        @Test
        @DisplayName("APPROVED path — calculates EMI and stores rate")
        void approvedPath() {
            Loan loan = appliedLoan();
            LoanDecisionRequest req = new LoanDecisionRequest("APPROVED", 10.5);
            when(repo.findById(1L)).thenReturn(Optional.of(loan));
            when(repo.save(any(Loan.class))).thenAnswer(inv -> inv.getArgument(0));

            LoanResponse resp = service.decideLoan(1L, req);

            assertThat(resp.getStatus()).isEqualTo("APPROVED");
            assertThat(resp.getInterestRate()).isEqualTo(10.5);
            assertThat(resp.getEmiAmount()).isNotNull().isGreaterThan(0.0);
        }

        @Test
        @DisplayName("REJECTED path — no EMI computation, status flipped")
        void rejectedPath() {
            Loan loan = appliedLoan();
            LoanDecisionRequest req = new LoanDecisionRequest("REJECTED", null);
            when(repo.findById(1L)).thenReturn(Optional.of(loan));
            when(repo.save(any(Loan.class))).thenAnswer(inv -> inv.getArgument(0));

            LoanResponse resp = service.decideLoan(1L, req);

            assertThat(resp.getStatus()).isEqualTo("REJECTED");
            assertThat(loan.getEmiAmount()).isNull();
            assertThat(loan.getInterestRate()).isNull();
        }

        @Test
        @DisplayName("throws BusinessException when already processed (not APPLIED)")
        void alreadyProcessed() {
            Loan loan = appliedLoan();
            loan.setStatus(LoanStatus.APPROVED);
            when(repo.findById(1L)).thenReturn(Optional.of(loan));

            assertThatThrownBy(() -> service.decideLoan(1L, new LoanDecisionRequest("APPROVED", 10.0)))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("already been processed");
        }

        @Test
        @DisplayName("throws BusinessException when APPROVED but no rate supplied")
        void approvedNoRate() {
            Loan loan = appliedLoan();
            when(repo.findById(1L)).thenReturn(Optional.of(loan));

            assertThatThrownBy(() -> service.decideLoan(1L, new LoanDecisionRequest("APPROVED", null)))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Interest rate is required");
        }

        @Test
        @DisplayName("throws ResourceNotFoundException when loan id is unknown")
        void notFound() {
            when(repo.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.decideLoan(99L, new LoanDecisionRequest("APPROVED", 10.0)))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // ────────────────────────────────────────────────────────────────────────
    //  disburse
    // ────────────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("disburse(...)")
    class Disburse {

        private Loan approvedLoan() {
            Loan l = new Loan();
            l.setLoanId(1L);
            l.setCustomerId("CUST1");
            l.setAccountId("ACC1");
            l.setLoanType(LoanType.HOME);
            l.setAmount(500_000.0);
            l.setRemainingAmount(500_000.0);
            l.setInterestRate(10.5);
            l.setTenureMonths(60);
            l.setEmiAmount(10_747.0);
            l.setStatus(LoanStatus.APPROVED);
            l.setEmiPaidCount(0);
            return l;
        }

        @Test
        @DisplayName("happy path — credits account, sets DISBURSED + nextDueDate")
        void happyPath() {
            Loan loan = approvedLoan();
            when(repo.findById(1L)).thenReturn(Optional.of(loan));
            when(repo.save(any(Loan.class))).thenAnswer(inv -> inv.getArgument(0));

            LoanResponse resp = service.disburse(1L);

            assertThat(resp.getStatus()).isEqualTo("DISBURSED");
            assertThat(loan.getDisbursedAt()).isNotNull();
            assertThat(loan.getNextDueDate()).isEqualTo(LocalDate.now().plusMonths(1));

            verify(accountClient).credit("ACC1", 500_000.0);
        }

        @Test
        @DisplayName("throws BusinessException when loan is not APPROVED")
        void notApproved() {
            Loan loan = approvedLoan();
            loan.setStatus(LoanStatus.APPLIED);
            when(repo.findById(1L)).thenReturn(Optional.of(loan));

            assertThatThrownBy(() -> service.disburse(1L))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("approved loans can be disbursed");

            verify(accountClient, never()).credit(anyString(), anyDouble());
        }
    }

    // ────────────────────────────────────────────────────────────────────────
    //  payEmi
    // ────────────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("payEmi(...)")
    class PayEmi {

        private Loan disbursedLoan() {
            Loan l = new Loan();
            l.setLoanId(1L);
            l.setCustomerId("CUST1");
            l.setAccountId("ACC1");
            l.setLoanType(LoanType.HOME);
            l.setAmount(500_000.0);
            l.setRemainingAmount(100_000.0);
            l.setInterestRate(10.5);
            l.setTenureMonths(60);
            l.setEmiAmount(10_747.0);
            l.setEmiPaidCount(40);
            l.setStatus(LoanStatus.DISBURSED);
            l.setNextDueDate(LocalDate.now().plusDays(5)); // not late
            return l;
        }

        @Test
        @DisplayName("happy path — on time, no penalty")
        void onTime() {
            Loan loan = disbursedLoan();
            EmiPaymentRequest req = new EmiPaymentRequest("ACC1", 10_747.0, "1234");
            when(repo.findById(1L)).thenReturn(Optional.of(loan));
            when(repo.save(any(Loan.class))).thenAnswer(inv -> inv.getArgument(0));

            LoanResponse resp = service.payEmi(1L, req);

            assertThat(resp.getStatus()).isEqualTo("DISBURSED");
            assertThat(loan.getEmiPaidCount()).isEqualTo(41);
            assertThat(loan.getRemainingAmount()).isLessThan(100_000.0);

            ArgumentCaptor<EmiPayment> payCaptor = ArgumentCaptor.forClass(EmiPayment.class);
            verify(emiPaymentRepo).save(payCaptor.capture());
            assertThat(payCaptor.getValue().isLate()).isFalse();
            assertThat(payCaptor.getValue().getPenaltyAmount()).isEqualTo(0.0);

            verify(accountClient).debitWithPin(eq("ACC1"), any(Map.class));
        }

        @Test
        @DisplayName("happy path — late payment applies 2% penalty")
        void latePayment() {
            Loan loan = disbursedLoan();
            loan.setNextDueDate(LocalDate.now().minusDays(2));
            EmiPaymentRequest req = new EmiPaymentRequest("ACC1", 10_747.0, "1234");
            when(repo.findById(1L)).thenReturn(Optional.of(loan));
            when(repo.save(any(Loan.class))).thenAnswer(inv -> inv.getArgument(0));

            service.payEmi(1L, req);

            ArgumentCaptor<EmiPayment> payCaptor = ArgumentCaptor.forClass(EmiPayment.class);
            verify(emiPaymentRepo).save(payCaptor.capture());
            assertThat(payCaptor.getValue().isLate()).isTrue();
            assertThat(payCaptor.getValue().getPenaltyAmount()).isGreaterThan(0.0);
        }

        @Test
        @DisplayName("closes loan when remaining hits 0")
        void closesLoan() {
            Loan loan = disbursedLoan();
            loan.setRemainingAmount(5_000.0);
            EmiPaymentRequest req = new EmiPaymentRequest("ACC1", 5_000.0, "1234");
            when(repo.findById(1L)).thenReturn(Optional.of(loan));
            when(repo.save(any(Loan.class))).thenAnswer(inv -> inv.getArgument(0));

            LoanResponse resp = service.payEmi(1L, req);

            assertThat(resp.getStatus()).isEqualTo("CLOSED");
            assertThat(loan.getRemainingAmount()).isEqualTo(0.0);
            assertThat(loan.getNextDueDate()).isNull();
        }

        @Test
        @DisplayName("throws BusinessException when loan not in DISBURSED state")
        void notDisbursed() {
            Loan loan = disbursedLoan();
            loan.setStatus(LoanStatus.APPROVED);
            EmiPaymentRequest req = new EmiPaymentRequest("ACC1", 10_000.0, "1234");
            when(repo.findById(1L)).thenReturn(Optional.of(loan));

            assertThatThrownBy(() -> service.payEmi(1L, req))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("not in disbursed state");

            verify(accountClient, never()).debitWithPin(anyString(), any());
        }

        @Test
        @DisplayName("throws BusinessException when amount exceeds remaining balance")
        void amountExceedsRemaining() {
            Loan loan = disbursedLoan();
            EmiPaymentRequest req = new EmiPaymentRequest("ACC1", 200_000.0, "1234");
            when(repo.findById(1L)).thenReturn(Optional.of(loan));

            assertThatThrownBy(() -> service.payEmi(1L, req))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("exceeds remaining balance");
        }
    }

    // ────────────────────────────────────────────────────────────────────────
    //  prepay
    // ────────────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("prepay(...)")
    class Prepay {

        private Loan disbursedLoan() {
            Loan l = new Loan();
            l.setLoanId(1L);
            l.setCustomerId("CUST1");
            l.setAccountId("ACC1");
            l.setLoanType(LoanType.HOME);
            l.setAmount(500_000.0);
            l.setRemainingAmount(100_000.0);
            l.setInterestRate(10.5);
            l.setTenureMonths(60);
            l.setEmiAmount(10_747.0);
            l.setEmiPaidCount(40);
            l.setStatus(LoanStatus.DISBURSED);
            l.setNextDueDate(LocalDate.now().plusDays(10));
            return l;
        }

        @Test
        @DisplayName("full foreclosure — applies 2% charge, closes the loan")
        void fullForeclosure() {
            Loan loan = disbursedLoan();
            PrepaymentRequest req = new PrepaymentRequest("ACC1", 100_000.0, true, "1234");
            when(repo.findById(1L)).thenReturn(Optional.of(loan));
            when(repo.save(any(Loan.class))).thenAnswer(inv -> inv.getArgument(0));

            PrepaymentResponse resp = service.prepay(1L, req);

            assertThat(resp.getStatus()).isEqualTo("CLOSED");
            assertThat(resp.getForeclosureCharge()).isEqualTo(2_000.0);
            assertThat(resp.getPrepaidAmount()).isEqualTo(100_000.0);
            assertThat(resp.getTotalDeducted()).isEqualTo(102_000.0);
            assertThat(resp.getRemainingBalance()).isEqualTo(0.0);
            assertThat(loan.getStatus()).isEqualTo(LoanStatus.CLOSED);
            assertThat(loan.getNextDueDate()).isNull();
        }

        @Test
        @DisplayName("partial prepay — EMI recalculated, loan remains DISBURSED")
        void partialPrepay() {
            Loan loan = disbursedLoan();
            Double originalEmi = loan.getEmiAmount();
            PrepaymentRequest req = new PrepaymentRequest("ACC1", 20_000.0, false, "1234");
            when(repo.findById(1L)).thenReturn(Optional.of(loan));
            when(repo.save(any(Loan.class))).thenAnswer(inv -> inv.getArgument(0));

            PrepaymentResponse resp = service.prepay(1L, req);

            assertThat(resp.getStatus()).isEqualTo("DISBURSED");
            assertThat(resp.getForeclosureCharge()).isEqualTo(0.0);
            assertThat(resp.getRemainingBalance()).isEqualTo(80_000.0);
            assertThat(loan.getEmiAmount()).isNotEqualTo(originalEmi);
            assertThat(loan.getStatus()).isEqualTo(LoanStatus.DISBURSED);
        }

        @Test
        @DisplayName("throws BusinessException when loan not in DISBURSED state")
        void notDisbursed() {
            Loan loan = disbursedLoan();
            loan.setStatus(LoanStatus.APPROVED);
            PrepaymentRequest req = new PrepaymentRequest("ACC1", 10_000.0, false, "1234");
            when(repo.findById(1L)).thenReturn(Optional.of(loan));

            assertThatThrownBy(() -> service.prepay(1L, req))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("only allowed for disbursed loans");
        }

        @Test
        @DisplayName("partial prepay — throws when amount exceeds remaining")
        void partialExceedsRemaining() {
            Loan loan = disbursedLoan();
            PrepaymentRequest req = new PrepaymentRequest("ACC1", 500_000.0, false, "1234");
            when(repo.findById(1L)).thenReturn(Optional.of(loan));

            assertThatThrownBy(() -> service.prepay(1L, req))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("exceeds remaining balance");
        }
    }

    // ────────────────────────────────────────────────────────────────────────
    //  checkEligibility
    // ────────────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("checkEligibility(...)")
    class CheckEligibility {

        private EligibilityCheckRequest req() {
            return new EligibilityCheckRequest("CUST1", 100_000.0, 500_000.0, 60);
        }

        @Test
        @DisplayName("eligible — within max loan amount")
        void eligible() {
            EligibilityCheckRequest req = req();
            when(customerClient.getCustomerDetails("CUST1")).thenReturn(activeCustomer);
            when(repo.getTotalActiveEmi("CUST1")).thenReturn(0.0);
            when(repo.getOutstanding("CUST1")).thenReturn(0.0);

            EligibilityResponse resp = service.checkEligibility(req);

            assertThat(resp.isEligible()).isTrue();
            assertThat(resp.getMaxAllowedEmi()).isEqualTo(50_000.0);
            assertThat(resp.getMaxEligibleAmount()).isGreaterThan(req.getRequestedAmount());
        }

        @Test
        @DisplayName("not eligible — customer not ACTIVE")
        void notActiveCustomer() {
            EligibilityCheckRequest req = req();
            when(customerClient.getCustomerDetails("CUST1")).thenReturn(inactiveCustomer);

            EligibilityResponse resp = service.checkEligibility(req);

            assertThat(resp.isEligible()).isFalse();
            assertThat(resp.getReason()).contains("not active");
        }

        @Test
        @DisplayName("not eligible — insufficient EMI capacity")
        void insufficientEmiCapacity() {
            EligibilityCheckRequest req = req();
            when(customerClient.getCustomerDetails("CUST1")).thenReturn(activeCustomer);
            when(repo.getTotalActiveEmi("CUST1")).thenReturn(60_000.0); // exceeds 50% of 100k
            when(repo.getOutstanding("CUST1")).thenReturn(200_000.0);

            EligibilityResponse resp = service.checkEligibility(req);

            assertThat(resp.isEligible()).isFalse();
            assertThat(resp.getReason()).contains("EMI obligations");
        }
    }

    // ────────────────────────────────────────────────────────────────────────
    //  getSchedule
    // ────────────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("getSchedule(...)")
    class GetSchedule {

        @Test
        @DisplayName("happy path — returns full amortisation schedule")
        void happyPath() {
            Loan loan = new Loan();
            loan.setLoanId(1L);
            loan.setAmount(500_000.0);
            loan.setInterestRate(10.5);
            loan.setTenureMonths(12);
            loan.setEmiAmount(44_055.0);
            loan.setStatus(LoanStatus.APPROVED);
            when(repo.findById(1L)).thenReturn(Optional.of(loan));

            EmiScheduleResponse resp = service.getSchedule(1L);

            assertThat(resp.getEmiAmount()).isEqualTo(44_055.0);
            assertThat(resp.getTotalMonths()).isEqualTo(12);
            assertThat(resp.getSchedule()).hasSize(12);
        }

        @Test
        @DisplayName("throws BusinessException when loan not approved (no EMI / no rate)")
        void notApproved() {
            Loan loan = new Loan();
            loan.setLoanId(1L);
            loan.setAmount(500_000.0);
            loan.setTenureMonths(12);
            loan.setStatus(LoanStatus.APPLIED);
            when(repo.findById(1L)).thenReturn(Optional.of(loan));

            assertThatThrownBy(() -> service.getSchedule(1L))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("not yet approved");
        }
    }
}
