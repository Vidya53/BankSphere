package com.cts.accountservice.service.impl;

import com.cts.accountservice.client.CustomerServiceClient;
import com.cts.accountservice.dto.request.AccountApplicationRequest;
import com.cts.accountservice.dto.request.RejectRequest;
import com.cts.accountservice.dto.response.AccountApplicationResponse;
import com.cts.accountservice.entity.Account;
import com.cts.accountservice.entity.AccountApplication;
import com.cts.accountservice.enums.AccountStatus;
import com.cts.accountservice.enums.AccountType;
import com.cts.accountservice.enums.ApplicationStatus;
import com.cts.accountservice.exception.DuplicateApplicationException;
import com.cts.accountservice.exception.InvalidOperationException;
import com.cts.accountservice.exception.KycNotApprovedException;
import com.cts.accountservice.exception.ResourceNotFoundException;
import com.cts.accountservice.exception.UnauthorizedBranchAccessException;
import com.cts.accountservice.repository.AccountApplicationRepository;
import com.cts.accountservice.repository.AccountRepository;
import com.cts.accountservice.context.UserContext;
import com.cts.accountservice.service.AuditService;
import com.cts.accountservice.service.BranchValidationService;
import com.cts.accountservice.service.KycVerificationService;
import com.cts.accountservice.service.NotificationService;
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

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pure JUnit 5 + Mockito test for {@link AccountApplicationServiceImpl}.
 *
 * No Spring context, no database, no Kafka — every collaborator is mocked.
 * Each test seeds only the stubs it needs (LENIENT strictness avoids
 * UnnecessaryStubbingException when failure branches short-circuit early).
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("AccountApplicationServiceImpl — business logic")
class AccountApplicationServiceImplTest {

    @Mock private AccountApplicationRepository applicationRepository;
    @Mock private AccountRepository accountRepository;
    @Mock private KycVerificationService kycVerificationService;
    @Mock private BranchValidationService branchValidationService;
    @Mock private NotificationService notificationService;
    @Mock private AuditService auditService;
    @Mock private CustomerServiceClient customerServiceClient;

    @InjectMocks private AccountApplicationServiceImpl service;

    private UserContext customerCtx;
    private UserContext csrCtx;

    @BeforeEach
    void setup() {
        customerCtx = new UserContext("USR1001", "cust@example.com", "CUSTOMER",
                "BR001", "Test Customer", "cust@example.com", "9876543210");
        csrCtx = new UserContext("STAFF42", "csr@example.com", "CSR",
                "BR001", "CSR User", "csr@example.com", null);
    }

    // ────────────────────────────────────────────────────────────────────────
    //  applyForAccount
    // ────────────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("applyForAccount(...)")
    class ApplyForAccount {

        @Test
        @DisplayName("happy path — saves a SUBMITTED application and fires audit + notification")
        void happyPath() {
            AccountApplicationRequest req = AccountApplicationRequest.builder()
                    .accountType(AccountType.SAVINGS)
                    .branchCode("BR001")
                    .initialDeposit(new BigDecimal("1000.00"))
                    .nomineeName("Alice")
                    .build();

            when(kycVerificationService.isKycApproved("USR1001")).thenReturn(true);
            when(branchValidationService.isBranchActive("BR001")).thenReturn(true);
            when(applicationRepository.existsByCustomerIdAndAccountTypeAndStatusIn(
                    eq("USR1001"), eq(AccountType.SAVINGS), anyList())).thenReturn(false);
            when(applicationRepository.save(any(AccountApplication.class)))
                    .thenAnswer(inv -> {
                        AccountApplication a = inv.getArgument(0);
                        a.setId(1L);
                        return a;
                    });

            AccountApplicationResponse response = service.applyForAccount(req, customerCtx);

            assertThat(response.getStatus()).isEqualTo(ApplicationStatus.SUBMITTED);
            assertThat(response.getApplicationRef()).startsWith("APP").hasSize(11);
            assertThat(response.getBranchCode()).isEqualTo("BR001");
            assertThat(response.getAccountType()).isEqualTo(AccountType.SAVINGS);

            ArgumentCaptor<AccountApplication> captor = ArgumentCaptor.forClass(AccountApplication.class);
            verify(applicationRepository).save(captor.capture());
            AccountApplication saved = captor.getValue();
            assertThat(saved.getCustomerId()).isEqualTo("USR1001");
            assertThat(saved.getCustomerName()).isEqualTo("Test Customer");
            assertThat(saved.getStatus()).isEqualTo(ApplicationStatus.SUBMITTED);
            assertThat(saved.getNomineeName()).isEqualTo("Alice");

            verify(auditService).logAudit(eq("USR1001"), eq("CUSTOMER"),
                    eq("ACCOUNT_APPLICATION_SUBMITTED"), eq("ACCOUNT_APPLICATION"),
                    anyString(), eq("BR001"));
            verify(notificationService).sendNotification(
                    eq("USR1001"), eq("cust@example.com"),
                    eq("Account Application Submitted"), contains("submitted successfully"));
        }

        @Test
        @DisplayName("rejects when KYC is not approved")
        void kycMissing() {
            AccountApplicationRequest req = AccountApplicationRequest.builder()
                    .accountType(AccountType.SAVINGS).branchCode("BR001").build();
            when(kycVerificationService.isKycApproved("USR1001")).thenReturn(false);

            assertThatThrownBy(() -> service.applyForAccount(req, customerCtx))
                    .isInstanceOf(KycNotApprovedException.class)
                    .hasMessageContaining("KYC must be approved");

            verify(applicationRepository, never()).save(any());
            verify(auditService, never()).logAudit(any(), any(), any(), any(), any(), any());
        }

        @Test
        @DisplayName("rejects when branch is inactive")
        void branchInactive() {
            AccountApplicationRequest req = AccountApplicationRequest.builder()
                    .accountType(AccountType.SAVINGS).branchCode("BR404").build();
            when(kycVerificationService.isKycApproved("USR1001")).thenReturn(true);
            when(branchValidationService.isBranchActive("BR404")).thenReturn(false);

            assertThatThrownBy(() -> service.applyForAccount(req, customerCtx))
                    .isInstanceOf(InvalidOperationException.class)
                    .hasMessageContaining("BR404")
                    .hasMessageContaining("not active");

            verify(applicationRepository, never()).save(any());
        }

        @Test
        @DisplayName("rejects when the customer already has a pending application for that type")
        void duplicatePending() {
            AccountApplicationRequest req = AccountApplicationRequest.builder()
                    .accountType(AccountType.SAVINGS).branchCode("BR001").build();
            when(kycVerificationService.isKycApproved("USR1001")).thenReturn(true);
            when(branchValidationService.isBranchActive("BR001")).thenReturn(true);
            when(applicationRepository.existsByCustomerIdAndAccountTypeAndStatusIn(
                    anyString(), any(), anyList())).thenReturn(true);

            assertThatThrownBy(() -> service.applyForAccount(req, customerCtx))
                    .isInstanceOf(DuplicateApplicationException.class)
                    .hasMessageContaining("pending");

            verify(applicationRepository, never()).save(any());
        }

        @Test
        @DisplayName("rejects SAVINGS application when deposit is below ₹500")
        void savingsBelowMinimum() {
            AccountApplicationRequest req = AccountApplicationRequest.builder()
                    .accountType(AccountType.SAVINGS).branchCode("BR001")
                    .initialDeposit(new BigDecimal("100.00")).build();
            when(kycVerificationService.isKycApproved(anyString())).thenReturn(true);
            when(branchValidationService.isBranchActive(anyString())).thenReturn(true);
            when(applicationRepository.existsByCustomerIdAndAccountTypeAndStatusIn(
                    anyString(), any(), anyList())).thenReturn(false);

            assertThatThrownBy(() -> service.applyForAccount(req, customerCtx))
                    .isInstanceOf(InvalidOperationException.class)
                    .hasMessageContaining("500");
        }

        @Test
        @DisplayName("rejects CURRENT application when deposit is below ₹5000")
        void currentBelowMinimum() {
            AccountApplicationRequest req = AccountApplicationRequest.builder()
                    .accountType(AccountType.CURRENT).branchCode("BR001")
                    .initialDeposit(new BigDecimal("1000.00")).build();
            when(kycVerificationService.isKycApproved(anyString())).thenReturn(true);
            when(branchValidationService.isBranchActive(anyString())).thenReturn(true);
            when(applicationRepository.existsByCustomerIdAndAccountTypeAndStatusIn(
                    anyString(), any(), anyList())).thenReturn(false);

            assertThatThrownBy(() -> service.applyForAccount(req, customerCtx))
                    .isInstanceOf(InvalidOperationException.class)
                    .hasMessageContaining("5000");
        }

        @Test
        @DisplayName("allows SAVINGS application with zero initial deposit")
        void savingsZeroDepositAllowed() {
            AccountApplicationRequest req = AccountApplicationRequest.builder()
                    .accountType(AccountType.SAVINGS).branchCode("BR001")
                    .initialDeposit(BigDecimal.ZERO).build();
            when(kycVerificationService.isKycApproved(anyString())).thenReturn(true);
            when(branchValidationService.isBranchActive(anyString())).thenReturn(true);
            when(applicationRepository.existsByCustomerIdAndAccountTypeAndStatusIn(
                    anyString(), any(), anyList())).thenReturn(false);
            when(applicationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            AccountApplicationResponse resp = service.applyForAccount(req, customerCtx);
            assertThat(resp.getStatus()).isEqualTo(ApplicationStatus.SUBMITTED);
        }
    }

    // ────────────────────────────────────────────────────────────────────────
    //  approveApplication
    // ────────────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("approveApplication(...)")
    class ApproveApplication {

        private AccountApplication submitted() {
            return AccountApplication.builder()
                    .id(1L)
                    .applicationRef("APP12345678")
                    .customerId("USR1001")
                    .customerName("Test Customer")
                    .customerEmail("cust@example.com")
                    .customerPhone("9876543210")
                    .branchCode("BR001")
                    .accountType(AccountType.SAVINGS)
                    .initialDeposit(new BigDecimal("1000.00"))
                    .status(ApplicationStatus.SUBMITTED)
                    .build();
        }

        @Test
        @DisplayName("happy path — creates ACTIVE account, marks app APPROVED, auto-activates customer")
        void happyPath() {
            AccountApplication app = submitted();
            when(applicationRepository.findById(1L)).thenReturn(Optional.of(app));
            when(kycVerificationService.isKycApproved("USR1001")).thenReturn(true);
            when(branchValidationService.getIfscCode("BR001")).thenReturn("BNKS0BR001");
            when(accountRepository.existsByAccountNo(anyString())).thenReturn(false);
            when(customerServiceClient.activateCustomerByUserId("USR1001")).thenReturn(true);
            when(applicationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(accountRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            AccountApplicationResponse response = service.approveApplication(1L, csrCtx);

            assertThat(response.getStatus()).isEqualTo(ApplicationStatus.APPROVED);
            assertThat(response.getGeneratedAccountNo()).startsWith("SAV").hasSize(17);
            assertThat(response.getReviewedBy()).isEqualTo("STAFF42");

            ArgumentCaptor<Account> accountCaptor = ArgumentCaptor.forClass(Account.class);
            verify(accountRepository).save(accountCaptor.capture());
            Account saved = accountCaptor.getValue();
            assertThat(saved.getStatus()).isEqualTo(AccountStatus.ACTIVE);
            assertThat(saved.getBalance()).isEqualByComparingTo("1000.00");
            assertThat(saved.getMinimumBalance()).isEqualByComparingTo("500.00");
            assertThat(saved.getIfscCode()).isEqualTo("BNKS0BR001");
            assertThat(saved.getApprovedBy()).isEqualTo("STAFF42");
            assertThat(saved.getIsTransactional()).isTrue();
            assertThat(saved.getDailyTransferLimit()).isEqualByComparingTo("500000.00");
            assertThat(saved.getDailyWithdrawalLimit()).isEqualByComparingTo("200000.00");

            verify(customerServiceClient).activateCustomerByUserId("USR1001");
            verify(auditService).logAudit(eq("STAFF42"), eq("CSR"),
                    eq("ACCOUNT_APPLICATION_APPROVED"), eq("ACCOUNT_APPLICATION"),
                    eq("APP12345678"), eq("BR001"));
            verify(auditService).logAudit(eq("STAFF42"), eq("CSR"),
                    eq("ACCOUNT_CREATED"), eq("ACCOUNT"),
                    anyString(), eq("BR001"));
            verify(notificationService).sendNotification(
                    eq("USR1001"), eq("cust@example.com"),
                    eq("Account Application Approved"), contains("approved"));
        }

        @Test
        @DisplayName("CURRENT account approval uses ₹5,000 minimum balance")
        void currentMinimumBalance() {
            AccountApplication app = submitted();
            app.setAccountType(AccountType.CURRENT);
            app.setInitialDeposit(new BigDecimal("10000.00"));
            when(applicationRepository.findById(1L)).thenReturn(Optional.of(app));
            when(kycVerificationService.isKycApproved("USR1001")).thenReturn(true);
            when(branchValidationService.getIfscCode("BR001")).thenReturn("BNKS0BR001");
            when(accountRepository.existsByAccountNo(anyString())).thenReturn(false);
            when(customerServiceClient.activateCustomerByUserId("USR1001")).thenReturn(true);
            when(applicationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(accountRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.approveApplication(1L, csrCtx);

            ArgumentCaptor<Account> captor = ArgumentCaptor.forClass(Account.class);
            verify(accountRepository).save(captor.capture());
            assertThat(captor.getValue().getMinimumBalance()).isEqualByComparingTo("5000.00");
            assertThat(captor.getValue().getAccountNo()).startsWith("CUR");
        }

        @Test
        @DisplayName("auto-activation failure must not roll back account creation")
        void autoActivateFailureIsSilent() {
            AccountApplication app = submitted();
            when(applicationRepository.findById(1L)).thenReturn(Optional.of(app));
            when(kycVerificationService.isKycApproved("USR1001")).thenReturn(true);
            when(branchValidationService.getIfscCode("BR001")).thenReturn("BNKS0BR001");
            when(accountRepository.existsByAccountNo(anyString())).thenReturn(false);
            when(customerServiceClient.activateCustomerByUserId("USR1001"))
                    .thenThrow(new RuntimeException("customer-service down"));
            when(applicationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(accountRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            AccountApplicationResponse response = service.approveApplication(1L, csrCtx);

            assertThat(response.getStatus()).isEqualTo(ApplicationStatus.APPROVED);
            verify(accountRepository).save(any());
            verify(notificationService).sendNotification(
                    eq("USR1001"), eq("cust@example.com"),
                    eq("Account Application Approved"), anyString());
        }

        @Test
        @DisplayName("throws ResourceNotFoundException when the application id is unknown")
        void notFound() {
            when(applicationRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.approveApplication(99L, csrCtx))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("99");
        }

        @Test
        @DisplayName("CSR from a different branch cannot approve")
        void wrongBranch() {
            AccountApplication app = submitted();
            app.setBranchCode("BR999");
            when(applicationRepository.findById(1L)).thenReturn(Optional.of(app));

            assertThatThrownBy(() -> service.approveApplication(1L, csrCtx))
                    .isInstanceOf(UnauthorizedBranchAccessException.class);

            verify(accountRepository, never()).save(any());
        }

        @Test
        @DisplayName("cannot approve an application that has already been APPROVED")
        void notReviewable() {
            AccountApplication app = submitted();
            app.setStatus(ApplicationStatus.APPROVED);
            when(applicationRepository.findById(1L)).thenReturn(Optional.of(app));

            assertThatThrownBy(() -> service.approveApplication(1L, csrCtx))
                    .isInstanceOf(InvalidOperationException.class)
                    .hasMessageContaining("reviewable");
        }

        @Test
        @DisplayName("approval is blocked if KYC has lapsed since submission")
        void kycRevoked() {
            AccountApplication app = submitted();
            when(applicationRepository.findById(1L)).thenReturn(Optional.of(app));
            when(kycVerificationService.isKycApproved("USR1001")).thenReturn(false);

            assertThatThrownBy(() -> service.approveApplication(1L, csrCtx))
                    .isInstanceOf(KycNotApprovedException.class)
                    .hasMessageContaining("no longer approved");

            verify(accountRepository, never()).save(any());
        }
    }

    // ────────────────────────────────────────────────────────────────────────
    //  rejectApplication
    // ────────────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("rejectApplication(...)")
    class RejectApplication {

        @Test
        @DisplayName("marks REJECTED, persists reason / remarks, notifies the customer")
        void happyPath() {
            AccountApplication app = AccountApplication.builder()
                    .id(1L).applicationRef("APP12345678")
                    .customerId("USR1001").customerName("Test")
                    .customerEmail("cust@example.com").branchCode("BR001")
                    .accountType(AccountType.SAVINGS)
                    .status(ApplicationStatus.SUBMITTED)
                    .build();
            RejectRequest req = new RejectRequest("Incomplete documentation submitted", "Mgr flagged");
            when(applicationRepository.findById(1L)).thenReturn(Optional.of(app));
            when(applicationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            AccountApplicationResponse resp = service.rejectApplication(1L, req, csrCtx);

            assertThat(resp.getStatus()).isEqualTo(ApplicationStatus.REJECTED);
            assertThat(resp.getRejectionReason()).isEqualTo("Incomplete documentation submitted");
            assertThat(resp.getReviewedBy()).isEqualTo("STAFF42");

            verify(notificationService).sendNotification(
                    eq("USR1001"), eq("cust@example.com"),
                    eq("Account Application Rejected"),
                    contains("Incomplete documentation submitted"));
            verify(auditService).logAudit(eq("STAFF42"), eq("CSR"),
                    eq("ACCOUNT_APPLICATION_REJECTED"), eq("ACCOUNT_APPLICATION"),
                    eq("APP12345678"), eq("BR001"));
        }

        @Test
        @DisplayName("cannot reject an application outside the caller's branch")
        void wrongBranch() {
            AccountApplication app = AccountApplication.builder()
                    .id(1L).branchCode("BR999")
                    .accountType(AccountType.SAVINGS)
                    .status(ApplicationStatus.SUBMITTED).build();
            when(applicationRepository.findById(1L)).thenReturn(Optional.of(app));

            assertThatThrownBy(() -> service.rejectApplication(1L, new RejectRequest("reason value", null), csrCtx))
                    .isInstanceOf(UnauthorizedBranchAccessException.class);

            verify(notificationService, never()).sendNotification(any(), any(), any(), any());
        }

        @Test
        @DisplayName("cannot reject an application that is already REJECTED")
        void notReviewable() {
            AccountApplication app = AccountApplication.builder()
                    .id(1L).branchCode("BR001")
                    .accountType(AccountType.SAVINGS)
                    .status(ApplicationStatus.REJECTED).build();
            when(applicationRepository.findById(1L)).thenReturn(Optional.of(app));

            assertThatThrownBy(() -> service.rejectApplication(1L, new RejectRequest("reason value", null), csrCtx))
                    .isInstanceOf(InvalidOperationException.class);
        }
    }

    // ────────────────────────────────────────────────────────────────────────
    //  getApplicationById — ownership enforcement
    // ────────────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("getApplicationById(...)")
    class GetApplicationById {

        @Test
        @DisplayName("a customer can read their own application")
        void ownerCanView() {
            AccountApplication app = AccountApplication.builder()
                    .id(1L).customerId("USR1001").branchCode("BR001")
                    .accountType(AccountType.SAVINGS)
                    .status(ApplicationStatus.SUBMITTED).build();
            when(applicationRepository.findById(1L)).thenReturn(Optional.of(app));

            AccountApplicationResponse resp = service.getApplicationById(1L, customerCtx);

            assertThat(resp.getCustomerId()).isEqualTo("USR1001");
        }

        @Test
        @DisplayName("a customer cannot read another customer's application")
        void otherCustomerBlocked() {
            AccountApplication app = AccountApplication.builder()
                    .id(1L).customerId("OTHER_USER").branchCode("BR001")
                    .accountType(AccountType.SAVINGS)
                    .status(ApplicationStatus.SUBMITTED).build();
            when(applicationRepository.findById(1L)).thenReturn(Optional.of(app));

            assertThatThrownBy(() -> service.getApplicationById(1L, customerCtx))
                    .isInstanceOf(UnauthorizedBranchAccessException.class)
                    .hasMessageContaining("own applications");
        }

        @Test
        @DisplayName("staff can read any application (no ownership check)")
        void staffCanView() {
            AccountApplication app = AccountApplication.builder()
                    .id(1L).customerId("USR1001").branchCode("BR001")
                    .accountType(AccountType.SAVINGS)
                    .status(ApplicationStatus.SUBMITTED).build();
            when(applicationRepository.findById(1L)).thenReturn(Optional.of(app));

            AccountApplicationResponse resp = service.getApplicationById(1L, csrCtx);

            assertThat(resp.getCustomerId()).isEqualTo("USR1001");
        }

        @Test
        @DisplayName("throws ResourceNotFoundException when the id does not exist")
        void notFound() {
            when(applicationRepository.findById(404L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getApplicationById(404L, customerCtx))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // ────────────────────────────────────────────────────────────────────────
    //  getMyApplications — listing happy path
    // ────────────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("getMyApplications(...)")
    class GetMyApplications {

        @Test
        @DisplayName("returns all applications for the caller in descending submission order")
        void listsApplications() {
            AccountApplication a = AccountApplication.builder()
                    .id(1L).applicationRef("APP00000001")
                    .customerId("USR1001").branchCode("BR001")
                    .accountType(AccountType.SAVINGS)
                    .status(ApplicationStatus.SUBMITTED).build();
            AccountApplication b = AccountApplication.builder()
                    .id(2L).applicationRef("APP00000002")
                    .customerId("USR1001").branchCode("BR001")
                    .accountType(AccountType.CURRENT)
                    .status(ApplicationStatus.APPROVED).build();
            when(applicationRepository.findByCustomerIdOrderByCreatedAtDesc("USR1001"))
                    .thenReturn(List.of(a, b));

            List<AccountApplicationResponse> resp = service.getMyApplications(customerCtx);

            assertThat(resp).hasSize(2);
            assertThat(resp).extracting(AccountApplicationResponse::getApplicationRef)
                    .containsExactly("APP00000001", "APP00000002");
        }

        @Test
        @DisplayName("returns an empty list when the customer has never applied")
        void empty() {
            when(applicationRepository.findByCustomerIdOrderByCreatedAtDesc("USR1001"))
                    .thenReturn(List.of());

            assertThat(service.getMyApplications(customerCtx)).isEmpty();
        }
    }
}
