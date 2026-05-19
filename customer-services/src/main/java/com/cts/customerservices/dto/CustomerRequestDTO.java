package com.cts.customerservices.dto;


import com.cts.customerservices.enums.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerRequestDTO {

    @NotBlank(message = "First name is required")
    @Size(min = 2, max = 50, message = "First name must be between 2 and 50 characters")
    @Pattern(regexp = "^[A-Za-z][A-Za-z .'-]{1,49}$", message = "First name may contain letters, spaces, hyphens and apostrophes only")
    private String firstName;

    @NotBlank(message = "Last name is required")
    @Size(min = 2, max = 50, message = "Last name must be between 2 and 50 characters")
    @Pattern(regexp = "^[A-Za-z][A-Za-z .'-]{1,49}$", message = "Last name may contain letters, spaces, hyphens and apostrophes only")
    private String lastName;

    @NotNull(message = "Date of birth is required")
    @Past(message = "Date of birth must be in the past")
    private LocalDate dateOfBirth;

    @NotNull(message = "Gender is required")
    private Gender gender;

    @NotBlank(message = "Email is required")
    @Email(message = "Please provide a valid email address")
    @Size(max = 254, message = "Email must not exceed 254 characters")
    private String email;

    @NotBlank(message = "Mobile number is required")
    @Pattern(
            regexp = "^[6-9][0-9]{9}$",
            message = "Mobile must be a 10-digit number starting with 6, 7, 8 or 9"
    )
    private String mobileNumber;

    @Pattern(
            regexp = "^$|^[6-9][0-9]{9}$",
            message = "Alternate mobile must be a 10-digit number starting with 6, 7, 8 or 9"
    )
    private String alternateMobileNumber;

    @PositiveOrZero(message = "Income amount cannot be negative")
    @DecimalMax(value = "100000000.00", message = "Income amount must not exceed 10 crore")
    private Double incomeAmount;

    @NotBlank(message = "Branch code is required")
    @Pattern(regexp = "^[A-Z0-9]{2,20}$", message = "Branch code must be 2-20 uppercase letters or digits")
    private String branchCode;

    @NotBlank(message = "Address Line 1 is required")
    @Size(min = 5, max = 100, message = "Address Line 1 must be between 5 and 100 characters")
    private String addressLine1;

    @Size(max = 100, message = "Address Line 2 must not exceed 100 characters")
    private String addressLine2;

    @NotBlank(message = "City is required")
    @Size(min = 2, max = 60, message = "City must be between 2 and 60 characters")
    private String city;

    @NotBlank(message = "State is required")
    @Size(min = 2, max = 60, message = "State must be between 2 and 60 characters")
    private String state;

    @NotBlank(message = "Postal code is required")
    @Pattern(regexp = "^[1-9][0-9]{5}$", message = "Postal code must be a 6-digit Indian PIN code")
    private String postalCode;

    @NotBlank(message = "Country is required")
    @Size(min = 2, max = 60, message = "Country must be between 2 and 60 characters")
    private String country;

}


