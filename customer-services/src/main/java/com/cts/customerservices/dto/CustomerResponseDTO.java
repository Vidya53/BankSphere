package com.cts.customerservices.dto;

import com.cts.customerservices.enums.*;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerResponseDTO {

    private String customerNo;

    private String firstName;

    private String lastName;

    private LocalDate dateOfBirth;

    private Gender gender;

    private String email;

    private String mobileNumber;

    private String alternateMobileNumber;

    private String branchCode;

    private String addressLine1;

    private String addressLine2;

    private String city;

    private String state;

    private String postalCode;

    private String country;

    private CustomerStatus status;

    private RiskCategory riskCategory;

    private Double incomeAmount;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

}
