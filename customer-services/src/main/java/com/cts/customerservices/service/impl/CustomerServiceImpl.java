package com.cts.customerservices.service.impl;


import com.cts.customerservices.client.BranchClient;
import com.cts.customerservices.dto.CustomerRequestDTO;
import com.cts.customerservices.dto.CustomerResponseDTO;
import com.cts.customerservices.dto.LoanApplicationRequest;
import com.cts.customerservices.dto.LoanEligibilityResponse;
import com.cts.customerservices.entity.Customer;
import com.cts.customerservices.enums.CustomerStatus;
import com.cts.customerservices.enums.KycStatus;
import com.cts.customerservices.enums.RiskCategory;
import com.cts.customerservices.exception.*;
import com.cts.customerservices.mapper.CustomerMapper;
import com.cts.customerservices.repository.CustomerRepository;
import com.cts.customerservices.repository.KycRepository;
import com.cts.customerservices.service.CustomerService;
import com.cts.customerservices.util.BusinessConstants;
import com.cts.customerservices.util.CustomerUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomerServiceImpl implements CustomerService {

    private final CustomerRepository repository;
    private final KycRepository kycRepository;
    private final BranchClient branchClient;

    @Override
    @Transactional
    public CustomerResponseDTO registerCustomer(CustomerRequestDTO request) {
        log.info("Registering new customer with email: {}", request.getEmail());

        validateBranch(request.getBranchCode());
        validateDuplicates(request);
        validateAge(request);
        validateAlternateMobile(request);

        String customerNo = CustomerUtil.generateCustomerNo();
        Customer customer = CustomerMapper.toEntity(request, customerNo);
        repository.save(customer);

        log.info("Customer registered successfully with customerNo: {}", customerNo);
        return CustomerMapper.toDTO(customer);
    }

    private void validateBranch(String branchCode) {
        try {
            if (!branchClient.isBranchActive(branchCode)) {
                throw new BranchNotActiveException(branchCode);
            }
        } catch (BranchNotActiveException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error while validating branch: {}", branchCode, e);
            throw new ServiceUnavailableException("Branch Service");
        }
    }

    private void validateDuplicates(CustomerRequestDTO request) {
        if (repository.existsByMobileNumber(request.getMobileNumber())) {
            throw new CustomerAlreadyExistsException(
                    "Mobile number [" + request.getMobileNumber() + "] is already registered with another customer"
            );
        }
        if (repository.existsByEmail(request.getEmail())) {
            throw new CustomerAlreadyExistsException(
                    "Email [" + request.getEmail() + "] is already registered with another customer"
            );
        }
    }

    private void validateAge(CustomerRequestDTO request) {
        LocalDate dob = request.getDateOfBirth();
        if (dob == null) return;

        int age = Period.between(dob, LocalDate.now()).getYears();
        if (age < BusinessConstants.MIN_CUSTOMER_AGE) {
            throw new BusinessException("Customer must be at least " + BusinessConstants.MIN_CUSTOMER_AGE + " years old. Current age: " + age);
        }
        if (age > BusinessConstants.MAX_CUSTOMER_AGE) {
            throw new BusinessException("Customer age [" + age + "] exceeds the maximum allowed age of " + BusinessConstants.MAX_CUSTOMER_AGE);
        }
    }

    private void validateAlternateMobile(CustomerRequestDTO request) {
        if (request.getAlternateMobileNumber() != null
                && !request.getAlternateMobileNumber().isBlank()
                && request.getAlternateMobileNumber().equals(request.getMobileNumber())) {
            throw new BusinessException("Alternate mobile number cannot be the same as primary mobile number");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public CustomerResponseDTO getCustomer(String customerNo) {
        log.debug("Fetching customer: {}", customerNo);
        Customer customer = findActiveCustomer(customerNo);
        return CustomerMapper.toDTO(customer);
    }

    @Override
    @Transactional
    public CustomerResponseDTO updateCustomer(String customerNo, CustomerRequestDTO request) {
        log.info("Updating customer: {}", customerNo);
        Customer customer = findActiveCustomer(customerNo);

        // Only allow update if customer is not CLOSED
        if (customer.getStatus() == CustomerStatus.CLOSED) {
            throw new BusinessException("Cannot update a CLOSED customer account");
        }

        customer.setAddressLine1(request.getAddressLine1());
        customer.setAddressLine2(request.getAddressLine2());
        customer.setCity(request.getCity());
        customer.setState(request.getState());
        customer.setPostalCode(request.getPostalCode());
        customer.setCountry(request.getCountry());
        customer.setAlternateMobileNumber(request.getAlternateMobileNumber());
        customer.setIncomeAmount(request.getIncomeAmount());
        customer.setUpdatedAt(LocalDateTime.now());
        customer.setUpdatedBy("SYSTEM");

        repository.save(customer);
        log.info("Customer {} updated successfully", customerNo);
        return CustomerMapper.toDTO(customer);
    }

    @Override
    @Transactional
    public void deleteCustomer(String customerNo) {
        log.info("Soft deleting customer: {}", customerNo);
        Customer customer = findCustomerByNo(customerNo);

        if (Boolean.TRUE.equals(customer.getIsDeleted())) {
            throw new CustomerDeletedException(customerNo);
        }

        if (customer.getStatus() == CustomerStatus.ACTIVE) {
            throw new BusinessException("Cannot delete an ACTIVE customer. Please deactivate the account first.");
        }

        customer.setIsDeleted(true);
        customer.setStatus(CustomerStatus.CLOSED);
        customer.setUpdatedAt(LocalDateTime.now());
        customer.setUpdatedBy("SYSTEM");
        customer.setRemarks("Soft deleted on " + LocalDateTime.now());
        repository.save(customer);
        log.info("Customer {} soft deleted successfully", customerNo);
    }

    @Override
    @Transactional
    public void activateCustomer(String customerNo) {
        log.info("Activating customer: {}", customerNo);
        updateStatus(customerNo, CustomerStatus.ACTIVE);
    }

    @Override
    @Transactional
    public void blockCustomer(String customerNo) {
        log.info("Blocking customer: {}", customerNo);
        updateStatus(customerNo, CustomerStatus.BLOCKED);
    }

    @Override
    @Transactional
    public void deactivateCustomer(String customerNo) {
        log.info("Deactivating customer: {}", customerNo);
        updateStatus(customerNo, CustomerStatus.INACTIVE);
    }

    private void updateStatus(String customerNo, CustomerStatus targetStatus) {
        Customer customer = findActiveCustomer(customerNo);
        CustomerStatus currentStatus = customer.getStatus();

        // Validate status transition
        Set<CustomerStatus> allowedTransitions = BusinessConstants.VALID_STATUS_TRANSITIONS.get(currentStatus);
        if (allowedTransitions == null || !allowedTransitions.contains(targetStatus)) {
            throw new InvalidStatusTransitionException(currentStatus.name(), targetStatus.name());
        }

        // For activation, verify KYC is approved
        if (targetStatus == CustomerStatus.ACTIVE && currentStatus == CustomerStatus.REGISTERED) {
            boolean isKycApproved = kycRepository.findByCustomerNo(customerNo)
                    .map(kyc -> kyc.getStatus() == KycStatus.APPROVED)
                    .orElse(false);
            if (!isKycApproved) {
                throw new KycNotVerifiedException(customerNo);
            }
        }

        customer.setStatus(targetStatus);
        customer.setUpdatedAt(LocalDateTime.now());
        customer.setUpdatedBy("SYSTEM");
        repository.save(customer);
        log.info("Customer {} status changed from {} to {}", customerNo, currentStatus, targetStatus);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CustomerResponseDTO> getAllCustomers() {
        return repository.findAll()
                .stream()
                .filter(c -> !Boolean.TRUE.equals(c.getIsDeleted()))
                .map(CustomerMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<CustomerResponseDTO> getCustomersByStatus(String status) {
        CustomerStatus st;
        try {
            st = CustomerStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException("Invalid customer status: [" + status + "]. Valid values are: REGISTERED, ACTIVE, INACTIVE, BLOCKED, CLOSED");
        }
        return repository.findByStatusAndIsDeletedFalse(st)
                .stream()
                .map(CustomerMapper::toDTO)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<CustomerResponseDTO> getCustomersByBranch(String branchCode) {
        if (branchCode == null || branchCode.isBlank()) {
            throw new BusinessException("Branch code cannot be empty");
        }
        return repository.findByBranchCode(branchCode)
                .stream()
                .filter(c -> !Boolean.TRUE.equals(c.getIsDeleted()))
                .map(CustomerMapper::toDTO)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<CustomerResponseDTO> getCustomersByCity(String city) {
        if (city == null || city.isBlank()) {
            throw new BusinessException("City name cannot be empty");
        }
        return repository.findByCity(city)
                .stream()
                .filter(c -> !Boolean.TRUE.equals(c.getIsDeleted()))
                .map(CustomerMapper::toDTO)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public CustomerResponseDTO getCustomerByMobile(String mobile) {
        if (mobile == null || !mobile.matches("^[6-9][0-9]{9}$")) {
            throw new BusinessException("Invalid mobile number format. Must be a 10-digit Indian number starting with 6-9");
        }
        Customer customer = repository.findByMobileNumber(mobile)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with mobile: " + mobile));
        checkNotDeleted(customer);
        return CustomerMapper.toDTO(customer);
    }

    @Override
    @Transactional(readOnly = true)
    public CustomerResponseDTO getCustomerByEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new BusinessException("Email cannot be empty");
        }
        Customer customer = repository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with email: " + email));
        checkNotDeleted(customer);
        return CustomerMapper.toDTO(customer);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CustomerResponseDTO> getCustomersCreatedBetween(LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null) {
            throw new BusinessException("Start and end dates are required");
        }
        if (start.isAfter(end)) {
            throw new BusinessException("Start date cannot be after end date");
        }
        return repository.findByCreatedAtBetween(start, end)
                .stream()
                .filter(c -> !Boolean.TRUE.equals(c.getIsDeleted()))
                .map(CustomerMapper::toDTO)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<CustomerResponseDTO> getHighRiskCustomers() {
        return repository.findByRiskCategory(RiskCategory.HIGH)
                .stream()
                .filter(c -> !Boolean.TRUE.equals(c.getIsDeleted()))
                .map(CustomerMapper::toDTO)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public LoanEligibilityResponse evaluateLoan(LoanApplicationRequest request) {
        log.info("Evaluating loan eligibility for customer: {}", request.getCustomerNo());
        Customer customer = findActiveCustomer(request.getCustomerNo());

        // Must be ACTIVE for loan eligibility
        if (customer.getStatus() != CustomerStatus.ACTIVE) {
            throw new CustomerNotActiveException(request.getCustomerNo());
        }

        List<String> reasons = new ArrayList<>();

        // 1. Policy Checks
        checkPolicy(customer, reasons);

        // 2. Income Check
        double income = (customer.getIncomeAmount() != null) ? customer.getIncomeAmount() : 0.0;
        if (income < BusinessConstants.MIN_INCOME_FOR_LOAN) {
            reasons.add(String.format("Minimum income of ₹%.0f is required for loan eligibility. Current income: ₹%.0f",
                    BusinessConstants.MIN_INCOME_FOR_LOAN, income));
        }

        // 3. EMI Calculation
        double emi = calculateEmi(request.getRequestedAmount(), request.getRepayDurationMonths());
        double maxEmiAllowed = income * BusinessConstants.MAX_DEBT_INCOME_RATIO;

        if (emi > maxEmiAllowed) {
            reasons.add(String.format("EMI ₹%.2f exceeds the allowed limit of ₹%.2f (45%% of monthly income)", emi, maxEmiAllowed));
        }

        boolean eligible = reasons.isEmpty();

        log.info("Loan evaluation result for {}: {}", request.getCustomerNo(), eligible ? "APPROVED" : "REJECTED");

        return LoanEligibilityResponse.builder()
                .isEligible(eligible)
                .decision(eligible ? "APPROVED" : "REJECTED")
                .calculatedEmi(Math.round(emi * 100.0) / 100.0)
                .maxAllowedEmi(Math.round(maxEmiAllowed * 100.0) / 100.0)
                .rejectionReasons(reasons)
                .build();
    }

    private void checkPolicy(Customer customer, List<String> reasons) {
        // Age Check
        int age = Period.between(customer.getDateOfBirth(), LocalDate.now()).getYears();
        if (age < BusinessConstants.MIN_CUSTOMER_AGE) {
            reasons.add("Customer must be at least " + BusinessConstants.MIN_CUSTOMER_AGE + " years old. Current age: " + age);
        }
        if (age > 65) {
            reasons.add("Loan eligibility is restricted for customers above 65 years. Current age: " + age);
        }

        // Status Check
        if (customer.getStatus() != CustomerStatus.ACTIVE) {
            reasons.add("Account is not in ACTIVE status. Current status: " + customer.getStatus());
        }

        // Risk Check
        if (customer.getRiskCategory() == RiskCategory.HIGH) {
            reasons.add("Customer risk category is HIGH. Lending is restricted for high-risk customers.");
        }

        // KYC Check
        boolean isKycVerified = kycRepository.findByCustomerNo(customer.getCustomerNo())
                .map(kyc -> kyc.getStatus() == KycStatus.APPROVED)
                .orElse(false);
        if (!isKycVerified) {
            reasons.add("KYC verification is required before loan evaluation.");
        }
    }

    private double calculateEmi(double p, int n) {
        double r = BusinessConstants.ANNUAL_INTEREST_RATE / 12;
        return (p * r * Math.pow(1 + r, n)) / (Math.pow(1 + r, n) - 1);
    }

    // ─── Helper Methods ───

    private Customer findCustomerByNo(String customerNo) {
        return repository.findByCustomerNo(customerNo)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with customerNo: " + customerNo));
    }

    private Customer findActiveCustomer(String customerNo) {
        Customer customer = findCustomerByNo(customerNo);
        checkNotDeleted(customer);
        return customer;
    }

    private void checkNotDeleted(Customer customer) {
        if (Boolean.TRUE.equals(customer.getIsDeleted())) {
            throw new CustomerDeletedException(customer.getCustomerNo());
        }
    }
}
