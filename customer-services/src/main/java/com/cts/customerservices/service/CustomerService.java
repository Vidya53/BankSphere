package com.cts.customerservices.service;

import com.cts.customerservices.dto.CustomerRequestDTO;
import com.cts.customerservices.dto.CustomerResponseDTO;
import com.cts.customerservices.dto.LoanApplicationRequest;
import com.cts.customerservices.dto.LoanEligibilityResponse;

import java.time.LocalDateTime;
import java.util.List;

public interface CustomerService {

    CustomerResponseDTO registerCustomer(
            CustomerRequestDTO request,
            String userId
    );

    CustomerResponseDTO getCustomer(
            String customerNo
    );

    CustomerResponseDTO getMyProfile(
            String userId
    );

    CustomerResponseDTO updateMyProfile(
            String userId,
            CustomerRequestDTO request
    );

    CustomerResponseDTO updateCustomer(
            String customerNo,
            CustomerRequestDTO request
    );

    void deleteCustomer(
            String customerNo
    );

    /**
     * Soft-deletes the customer identified by the identity-service userId.
     * Convenience for admin tools (e.g. the branch view modal) that hold the
     * userId from an Account row but don't already know the customerNo.
     */
    void deleteCustomerByUserId(
            String userId
    );

    void activateCustomer(
            String customerNo
    );

    void blockCustomer(
            String customerNo
    );

    void deactivateCustomer(
            String customerNo
    );

    List<CustomerResponseDTO> getAllCustomers();

    List<CustomerResponseDTO> getCustomersByStatus(
            String status
    );

    List<CustomerResponseDTO> getCustomersByBranch(
            String branchCode
    );

    List<CustomerResponseDTO> getCustomersByCity(
            String city
    );

    CustomerResponseDTO getCustomerByMobile(
            String mobile
    );

    CustomerResponseDTO getCustomerByEmail(
            String email
    );

    List<CustomerResponseDTO> getCustomersCreatedBetween(
            LocalDateTime start,
            LocalDateTime end
    );

    List<CustomerResponseDTO> getHighRiskCustomers();

    public LoanEligibilityResponse evaluateLoan(LoanApplicationRequest request);
}

