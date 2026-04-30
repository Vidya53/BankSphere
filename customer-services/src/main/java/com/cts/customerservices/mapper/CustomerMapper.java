package com.cts.customerservices.mapper;



import com.cts.customerservices.dto.CustomerRequestDTO;
import com.cts.customerservices.dto.CustomerResponseDTO;
import com.cts.customerservices.entity.Customer;
import com.cts.customerservices.enums.CustomerStatus;
import com.cts.customerservices.enums.RiskCategory;

import java.time.LocalDateTime;

public class CustomerMapper {

    public static Customer toEntity(
            CustomerRequestDTO dto,
            String customerNo
    ) {

        return Customer.builder()
                .customerNo(customerNo)
                .firstName(dto.getFirstName())
                .lastName(dto.getLastName())
                .dateOfBirth(dto.getDateOfBirth())
                .gender(dto.getGender())
                .email(dto.getEmail())
                .mobileNumber(dto.getMobileNumber())
                .alternateMobileNumber(dto.getAlternateMobileNumber())
                .branchCode(dto.getBranchCode())
                .addressLine1(dto.getAddressLine1())
                .addressLine2(dto.getAddressLine2())
                .city(dto.getCity())
                .state(dto.getState())
                .postalCode(dto.getPostalCode())
                .country(dto.getCountry())
                .incomeAmount(dto.getIncomeAmount())
                .riskCategory(RiskCategory.LOW)
                .status(CustomerStatus.REGISTERED)
                .createdAt(LocalDateTime.now())
                .isDeleted(false)
                .build();

    }

    public static CustomerResponseDTO toDTO(Customer customer) {

        return CustomerResponseDTO.builder()
                .customerNo(customer.getCustomerNo())
                .firstName(customer.getFirstName())
                .lastName(customer.getLastName())
                .dateOfBirth(customer.getDateOfBirth())
                .gender(customer.getGender())
                .email(customer.getEmail())
                .mobileNumber(customer.getMobileNumber())
                .alternateMobileNumber(customer.getAlternateMobileNumber())
                .incomeAmount(customer.getIncomeAmount())
                .branchCode(customer.getBranchCode())
                .addressLine1(customer.getAddressLine1())
                .addressLine2(customer.getAddressLine2())
                .city(customer.getCity())
                .state(customer.getState())
                .postalCode(customer.getPostalCode())
                .country(customer.getCountry())
                .status(customer.getStatus())
                .riskCategory(customer.getRiskCategory())
                .createdAt(customer.getCreatedAt())
                .updatedAt(customer.getUpdatedAt())
                .build();

    }

}
