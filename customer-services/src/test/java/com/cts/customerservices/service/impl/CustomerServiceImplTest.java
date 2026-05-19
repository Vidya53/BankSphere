package com.cts.customerservices.service.impl;

import com.cts.customerservices.client.AccountServiceClient;
import com.cts.customerservices.client.BranchClient;
import com.cts.customerservices.dto.CustomerRequestDTO;
import com.cts.customerservices.dto.CustomerResponseDTO;
import com.cts.customerservices.dto.LoanApplicationRequest;
import com.cts.customerservices.dto.LoanEligibilityResponse;
import com.cts.customerservices.entity.Customer;
import com.cts.customerservices.entity.Kyc;
import com.cts.customerservices.enums.CustomerStatus;
import com.cts.customerservices.enums.Gender;
import com.cts.customerservices.enums.KycStatus;
import com.cts.customerservices.enums.RiskCategory;
import com.cts.customerservices.exception.BranchNotActiveException;
import com.cts.customerservices.exception.BusinessException;
import com.cts.customerservices.exception.CustomerAlreadyExistsException;
import com.cts.customerservices.exception.CustomerDeletedException;
import com.cts.customerservices.exception.CustomerNotActiveException;
import com.cts.customerservices.exception.InvalidStatusTransitionException;
import com.cts.customerservices.exception.KycNotVerifiedException;
import com.cts.customerservices.exception.ResourceNotFoundException;
import com.cts.customerservices.repository.CustomerRepository;
import com.cts.customerservices.repository.KycRepository;
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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pure JUnit 5 + Mockito test for {@link CustomerServiceImpl}.
 *
 * No Spring context, no database, no Feign — every collaborator is mocked.
 * LENIENT strictness avoids UnnecessaryStubbingException for failure-branch
 * tests that short-circuit before reaching the deeper stubs.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("CustomerServiceImpl — business logic")
class CustomerServiceImplTest {

    @Mock private CustomerRepository repository;
    @Mock private KycRepository kycRepository;
    @Mock private BranchClient branchClient;
    @Mock private AccountServiceClient accountServiceClient;

    @InjectMocks private CustomerServiceImpl service;

    private CustomerRequestDTO baseRequest() {
        return CustomerRequestDTO.builder()
                .firstName("John")
                .lastName("Doe")
                .dateOfBirth(LocalDate.of(1990, 1, 1))
                .gender(Gender.MALE)
                .email("john@example.com")
                .mobileNumber("9876543210")
                .alternateMobileNumber(null)
                .incomeAmount(50000.0)
                .branchCode("BR001")
                .addressLine1("123 Main St")
                .addressLine2(null)
                .city("Mumbai")
                .state("MH")
                .postalCode("400001")
                .country("India")
                .build();
    }

    private Customer baseCustomer() {
        return Customer.builder()
                .id(1L)
                .customerNo("CUST-AB12CD34")
                .userId("USR1001")
                .firstName("John")
                .lastName("Doe")
                .dateOfBirth(LocalDate.of(1990, 1, 1))
                .gender(Gender.MALE)
                .email("john@example.com")
                .mobileNumber("9876543210")
                .branchCode("BR001")
                .addressLine1("123 Main St")
                .city("Mumbai")
                .state("MH")
                .postalCode("400001")
                .country("India")
                .status(CustomerStatus.ACTIVE)
                .riskCategory(RiskCategory.LOW)
                .incomeAmount(50000.0)
                .isDeleted(false)
                .build();
    }

    @BeforeEach
    void setup() {
        // No-op — each test seeds its own stubs.
    }

    // ────────────────────────────────────────────────────────────────────────
    //  registerCustomer
    // ────────────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("registerCustomer(...)")
    class RegisterCustomer {

        @Test
        @DisplayName("happy path — persists a REGISTERED customer with generated customerNo")
        void happyPath() {
            CustomerRequestDTO req = baseRequest();
            when(branchClient.isBranchActive("BR001")).thenReturn(true);
            when(repository.existsByMobileNumber("9876543210")).thenReturn(false);
            when(repository.existsByEmail("john@example.com")).thenReturn(false);
            when(repository.save(any(Customer.class))).thenAnswer(inv -> inv.getArgument(0));

            CustomerResponseDTO resp = service.registerCustomer(req, "USR1001");

            assertThat(resp.getEmail()).isEqualTo("john@example.com");
            assertThat(resp.getMobileNumber()).isEqualTo("9876543210");
            assertThat(resp.getStatus()).isEqualTo(CustomerStatus.REGISTERED);
            assertThat(resp.getRiskCategory()).isEqualTo(RiskCategory.LOW);

            ArgumentCaptor<Customer> captor = ArgumentCaptor.forClass(Customer.class);
            verify(repository).save(captor.capture());
            Customer saved = captor.getValue();
            assertThat(saved.getUserId()).isEqualTo("USR1001");
            assertThat(saved.getCustomerNo()).startsWith("CUST-");
            assertThat(saved.getStatus()).isEqualTo(CustomerStatus.REGISTERED);
        }

        @Test
        @DisplayName("rejects when the branch is inactive")
        void branchInactive() {
            CustomerRequestDTO req = baseRequest();
            req.setBranchCode("BR404");
            when(branchClient.isBranchActive("BR404")).thenReturn(false);

            assertThatThrownBy(() -> service.registerCustomer(req, "USR1001"))
                    .isInstanceOf(BranchNotActiveException.class)
                    .hasMessageContaining("BR404");

            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("rejects on duplicate mobile number")
        void duplicateMobile() {
            CustomerRequestDTO req = baseRequest();
            when(branchClient.isBranchActive("BR001")).thenReturn(true);
            when(repository.existsByMobileNumber("9876543210")).thenReturn(true);

            assertThatThrownBy(() -> service.registerCustomer(req, "USR1001"))
                    .isInstanceOf(CustomerAlreadyExistsException.class)
                    .hasMessageContaining("Mobile number")
                    .hasMessageContaining("9876543210");

            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("rejects on duplicate email")
        void duplicateEmail() {
            CustomerRequestDTO req = baseRequest();
            when(branchClient.isBranchActive("BR001")).thenReturn(true);
            when(repository.existsByMobileNumber("9876543210")).thenReturn(false);
            when(repository.existsByEmail("john@example.com")).thenReturn(true);

            assertThatThrownBy(() -> service.registerCustomer(req, "USR1001"))
                    .isInstanceOf(CustomerAlreadyExistsException.class)
                    .hasMessageContaining("Email")
                    .hasMessageContaining("john@example.com");

            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("rejects when the customer is under 18")
        void ageBelowMinimum() {
            CustomerRequestDTO req = baseRequest();
            req.setDateOfBirth(LocalDate.now().minusYears(15));
            when(branchClient.isBranchActive("BR001")).thenReturn(true);
            when(repository.existsByMobileNumber(anyString())).thenReturn(false);
            when(repository.existsByEmail(anyString())).thenReturn(false);

            assertThatThrownBy(() -> service.registerCustomer(req, "USR1001"))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("at least 18");

            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("rejects when the customer is over 100")
        void ageAboveMaximum() {
            CustomerRequestDTO req = baseRequest();
            req.setDateOfBirth(LocalDate.now().minusYears(110));
            when(branchClient.isBranchActive("BR001")).thenReturn(true);
            when(repository.existsByMobileNumber(anyString())).thenReturn(false);
            when(repository.existsByEmail(anyString())).thenReturn(false);

            assertThatThrownBy(() -> service.registerCustomer(req, "USR1001"))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("exceeds the maximum");

            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("rejects when alternate mobile equals primary mobile")
        void alternateMobileEqualsPrimary() {
            CustomerRequestDTO req = baseRequest();
            req.setAlternateMobileNumber("9876543210");
            when(branchClient.isBranchActive("BR001")).thenReturn(true);
            when(repository.existsByMobileNumber(anyString())).thenReturn(false);
            when(repository.existsByEmail(anyString())).thenReturn(false);

            assertThatThrownBy(() -> service.registerCustomer(req, "USR1001"))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Alternate mobile");

            verify(repository, never()).save(any());
        }
    }

    // ────────────────────────────────────────────────────────────────────────
    //  getMyProfile
    // ────────────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("getMyProfile(...)")
    class GetMyProfile {

        @Test
        @DisplayName("happy path — returns the caller's profile")
        void happyPath() {
            Customer c = baseCustomer();
            when(repository.findByUserId("USR1001")).thenReturn(Optional.of(c));

            CustomerResponseDTO resp = service.getMyProfile("USR1001");

            assertThat(resp.getCustomerNo()).isEqualTo("CUST-AB12CD34");
            assertThat(resp.getEmail()).isEqualTo("john@example.com");
        }

        @Test
        @DisplayName("throws ResourceNotFoundException when no profile exists for the userId")
        void notFound() {
            when(repository.findByUserId("USR999")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getMyProfile("USR999"))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("USR999");
        }

        @Test
        @DisplayName("throws CustomerDeletedException when the profile is soft-deleted")
        void softDeleted() {
            Customer c = baseCustomer();
            c.setIsDeleted(true);
            when(repository.findByUserId("USR1001")).thenReturn(Optional.of(c));

            assertThatThrownBy(() -> service.getMyProfile("USR1001"))
                    .isInstanceOf(CustomerDeletedException.class)
                    .hasMessageContaining("CUST-AB12CD34");
        }
    }

    // ────────────────────────────────────────────────────────────────────────
    //  updateMyProfile
    // ────────────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("updateMyProfile(...)")
    class UpdateMyProfile {

        @Test
        @DisplayName("happy path — persists editable fields")
        void happyPath() {
            Customer c = baseCustomer();
            when(repository.findByUserId("USR1001")).thenReturn(Optional.of(c));
            when(repository.save(any(Customer.class))).thenAnswer(inv -> inv.getArgument(0));

            CustomerRequestDTO req = baseRequest();
            req.setFirstName("Johnny");
            req.setCity("Pune");

            CustomerResponseDTO resp = service.updateMyProfile("USR1001", req);

            assertThat(resp.getFirstName()).isEqualTo("Johnny");
            assertThat(resp.getCity()).isEqualTo("Pune");
            verify(repository).save(c);
        }

        @Test
        @DisplayName("rejects when the customer is CLOSED")
        void rejectClosed() {
            Customer c = baseCustomer();
            c.setStatus(CustomerStatus.CLOSED);
            when(repository.findByUserId("USR1001")).thenReturn(Optional.of(c));

            assertThatThrownBy(() -> service.updateMyProfile("USR1001", baseRequest()))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("CLOSED");

            verify(repository, never()).save(any());
        }
    }

    // ────────────────────────────────────────────────────────────────────────
    //  deleteCustomer
    // ────────────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("deleteCustomer(...)")
    class DeleteCustomer {

        @Test
        @DisplayName("happy path — soft-deletes the customer and cascade-closes accounts")
        void happyPath() {
            Customer c = baseCustomer();
            when(repository.findByCustomerNo("CUST-AB12CD34")).thenReturn(Optional.of(c));
            when(repository.save(any(Customer.class))).thenAnswer(inv -> inv.getArgument(0));
            when(accountServiceClient.closeAllAccountsForCustomer(eq("USR1001"), anyString(), eq("SYSTEM")))
                    .thenReturn(3);

            service.deleteCustomer("CUST-AB12CD34");

            assertThat(c.getIsDeleted()).isTrue();
            assertThat(c.getStatus()).isEqualTo(CustomerStatus.CLOSED);
            verify(repository).save(c);
            verify(accountServiceClient).closeAllAccountsForCustomer(
                    eq("USR1001"), anyString(), eq("SYSTEM"));
        }

        @Test
        @DisplayName("throws CustomerDeletedException when already soft-deleted")
        void alreadyDeleted() {
            Customer c = baseCustomer();
            c.setIsDeleted(true);
            when(repository.findByCustomerNo("CUST-AB12CD34")).thenReturn(Optional.of(c));

            assertThatThrownBy(() -> service.deleteCustomer("CUST-AB12CD34"))
                    .isInstanceOf(CustomerDeletedException.class)
                    .hasMessageContaining("CUST-AB12CD34");

            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("delete still succeeds when account-service cascade-close fails")
        void cascadeFailureIsSwallowed() {
            Customer c = baseCustomer();
            when(repository.findByCustomerNo("CUST-AB12CD34")).thenReturn(Optional.of(c));
            when(repository.save(any(Customer.class))).thenAnswer(inv -> inv.getArgument(0));
            when(accountServiceClient.closeAllAccountsForCustomer(anyString(), anyString(), anyString()))
                    .thenThrow(new RuntimeException("account-service unavailable"));

            service.deleteCustomer("CUST-AB12CD34");

            assertThat(c.getIsDeleted()).isTrue();
            assertThat(c.getStatus()).isEqualTo(CustomerStatus.CLOSED);
            verify(repository).save(c);
        }
    }

    // ────────────────────────────────────────────────────────────────────────
    //  activateCustomer
    // ────────────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("activateCustomer(...)")
    class ActivateCustomer {

        @Test
        @DisplayName("happy path — REGISTERED → ACTIVE with KYC approved")
        void happyPath() {
            Customer c = baseCustomer();
            c.setStatus(CustomerStatus.REGISTERED);
            when(repository.findByCustomerNo("CUST-AB12CD34")).thenReturn(Optional.of(c));
            when(repository.save(any(Customer.class))).thenAnswer(inv -> inv.getArgument(0));

            Kyc kyc = Kyc.builder().status(KycStatus.APPROVED).build();
            when(kycRepository.findByCustomerNo("CUST-AB12CD34")).thenReturn(Optional.of(kyc));

            service.activateCustomer("CUST-AB12CD34");

            assertThat(c.getStatus()).isEqualTo(CustomerStatus.ACTIVE);
            verify(repository).save(c);
        }

        @Test
        @DisplayName("throws KycNotVerifiedException when KYC is not approved")
        void kycNotApproved() {
            Customer c = baseCustomer();
            c.setStatus(CustomerStatus.REGISTERED);
            when(repository.findByCustomerNo("CUST-AB12CD34")).thenReturn(Optional.of(c));
            when(kycRepository.findByCustomerNo("CUST-AB12CD34")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.activateCustomer("CUST-AB12CD34"))
                    .isInstanceOf(KycNotVerifiedException.class)
                    .hasMessageContaining("CUST-AB12CD34");

            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("throws InvalidStatusTransitionException for an invalid transition (CLOSED → ACTIVE)")
        void invalidTransition() {
            Customer c = baseCustomer();
            c.setStatus(CustomerStatus.CLOSED);
            when(repository.findByCustomerNo("CUST-AB12CD34")).thenReturn(Optional.of(c));

            assertThatThrownBy(() -> service.activateCustomer("CUST-AB12CD34"))
                    .isInstanceOf(InvalidStatusTransitionException.class);

            verify(repository, never()).save(any());
        }
    }

    // ────────────────────────────────────────────────────────────────────────
    //  blockCustomer / deactivateCustomer
    // ────────────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("blockCustomer / deactivateCustomer")
    class StatusChanges {

        @Test
        @DisplayName("blockCustomer — happy path ACTIVE → BLOCKED")
        void block() {
            Customer c = baseCustomer();
            c.setStatus(CustomerStatus.ACTIVE);
            when(repository.findByCustomerNo("CUST-AB12CD34")).thenReturn(Optional.of(c));
            when(repository.save(any(Customer.class))).thenAnswer(inv -> inv.getArgument(0));

            service.blockCustomer("CUST-AB12CD34");

            assertThat(c.getStatus()).isEqualTo(CustomerStatus.BLOCKED);
            verify(repository).save(c);
        }

        @Test
        @DisplayName("deactivateCustomer — happy path ACTIVE → INACTIVE")
        void deactivate() {
            Customer c = baseCustomer();
            c.setStatus(CustomerStatus.ACTIVE);
            when(repository.findByCustomerNo("CUST-AB12CD34")).thenReturn(Optional.of(c));
            when(repository.save(any(Customer.class))).thenAnswer(inv -> inv.getArgument(0));

            service.deactivateCustomer("CUST-AB12CD34");

            assertThat(c.getStatus()).isEqualTo(CustomerStatus.INACTIVE);
            verify(repository).save(c);
        }
    }

    // ────────────────────────────────────────────────────────────────────────
    //  evaluateLoan
    // ────────────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("evaluateLoan(...)")
    class EvaluateLoan {

        private LoanApplicationRequest loanRequest() {
            LoanApplicationRequest req = new LoanApplicationRequest();
            req.setCustomerNo("CUST-AB12CD34");
            req.setRequestedAmount(200000.0);
            req.setRepayDurationMonths(36);
            req.setLoanPurpose("Home renovation");
            return req;
        }

        @Test
        @DisplayName("happy path — eligible customer is APPROVED")
        void eligible() {
            Customer c = baseCustomer();
            c.setIncomeAmount(100000.0);
            when(repository.findByCustomerNo("CUST-AB12CD34")).thenReturn(Optional.of(c));
            Kyc kyc = Kyc.builder().status(KycStatus.APPROVED).build();
            when(kycRepository.findByCustomerNo("CUST-AB12CD34")).thenReturn(Optional.of(kyc));

            LoanEligibilityResponse resp = service.evaluateLoan(loanRequest());

            assertThat(resp.isEligible()).isTrue();
            assertThat(resp.getDecision()).isEqualTo("APPROVED");
            assertThat(resp.getRejectionReasons()).isEmpty();
            assertThat(resp.getCalculatedEmi()).isPositive();
        }

        @Test
        @DisplayName("rejects when customer status is not ACTIVE")
        void rejectsInactive() {
            Customer c = baseCustomer();
            c.setStatus(CustomerStatus.INACTIVE);
            when(repository.findByCustomerNo("CUST-AB12CD34")).thenReturn(Optional.of(c));

            assertThatThrownBy(() -> service.evaluateLoan(loanRequest()))
                    .isInstanceOf(CustomerNotActiveException.class);
        }

        @Test
        @DisplayName("rejects HIGH risk customer")
        void rejectsHighRisk() {
            Customer c = baseCustomer();
            c.setIncomeAmount(100000.0);
            c.setRiskCategory(RiskCategory.HIGH);
            when(repository.findByCustomerNo("CUST-AB12CD34")).thenReturn(Optional.of(c));
            Kyc kyc = Kyc.builder().status(KycStatus.APPROVED).build();
            when(kycRepository.findByCustomerNo("CUST-AB12CD34")).thenReturn(Optional.of(kyc));

            LoanEligibilityResponse resp = service.evaluateLoan(loanRequest());

            assertThat(resp.isEligible()).isFalse();
            assertThat(resp.getDecision()).isEqualTo("REJECTED");
            assertThat(resp.getRejectionReasons())
                    .anyMatch(r -> r.toLowerCase().contains("high"));
        }

        @Test
        @DisplayName("rejects when income is below the minimum threshold")
        void rejectsLowIncome() {
            Customer c = baseCustomer();
            c.setIncomeAmount(5000.0);
            when(repository.findByCustomerNo("CUST-AB12CD34")).thenReturn(Optional.of(c));
            Kyc kyc = Kyc.builder().status(KycStatus.APPROVED).build();
            when(kycRepository.findByCustomerNo("CUST-AB12CD34")).thenReturn(Optional.of(kyc));

            LoanEligibilityResponse resp = service.evaluateLoan(loanRequest());

            assertThat(resp.isEligible()).isFalse();
            assertThat(resp.getRejectionReasons())
                    .anyMatch(r -> r.toLowerCase().contains("minimum income"));
        }

        @Test
        @DisplayName("rejects when EMI exceeds 45% of monthly income")
        void rejectsEmiTooHigh() {
            Customer c = baseCustomer();
            // High enough income to clear the floor, but small enough that the
            // ₹200k / 36-month EMI breaches the 45% debt-income ceiling.
            c.setIncomeAmount(12000.0);
            when(repository.findByCustomerNo("CUST-AB12CD34")).thenReturn(Optional.of(c));
            Kyc kyc = Kyc.builder().status(KycStatus.APPROVED).build();
            when(kycRepository.findByCustomerNo("CUST-AB12CD34")).thenReturn(Optional.of(kyc));

            LoanEligibilityResponse resp = service.evaluateLoan(loanRequest());

            assertThat(resp.isEligible()).isFalse();
            assertThat(resp.getRejectionReasons())
                    .anyMatch(r -> r.contains("EMI"));
        }
    }
}
