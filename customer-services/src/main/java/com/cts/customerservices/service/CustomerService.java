package com.cts.customerservices.service;

import com.cts.customerservices.dto.CustomerRequestDTO;
import com.cts.customerservices.dto.CustomerResponseDTO;
import com.cts.customerservices.dto.LoanApplicationRequest;
import com.cts.customerservices.dto.LoanEligibilityResponse;

import java.time.LocalDateTime;
import java.util.List;

public interface CustomerService {

    CustomerResponseDTO registerCustomer(
            CustomerRequestDTO request
    );

    CustomerResponseDTO getCustomer(
            String customerNo
    );

    CustomerResponseDTO updateCustomer(
            String customerNo,
            CustomerRequestDTO request
    );

    void deleteCustomer(
            String customerNo
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

