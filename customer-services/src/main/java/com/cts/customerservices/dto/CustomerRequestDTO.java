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
    @Pattern(regexp = "^[a-zA-Z\\s]+$", message = "First name must contain only alphabets")
    private String firstName;

    @NotBlank(message = "Last name is required")
    @Size(min = 1, max = 50, message = "Last name must be between 1 and 50 characters")
    @Pattern(regexp = "^[a-zA-Z\\s]+$", message = "Last name must contain only alphabets")
    private String lastName;

    @NotNull(message = "Date of birth is required")
    @Past(message = "Date of birth must be in the past")
    private LocalDate dateOfBirth;

    @NotNull(message = "Gender is required")
    private Gender gender;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    @Size(max = 100, message = "Email must not exceed 100 characters")
    private String email;

    @NotBlank(message = "Mobile number is required")
    @Pattern(
            regexp = "^[6-9][0-9]{9}$",
            message = "Mobile number must be a valid 10-digit Indian number starting with 6-9"
    )
    private String mobileNumber;

    @Pattern(
            regexp = "^$|^[6-9][0-9]{9}$",
            message = "Alternate mobile number must be a valid 10-digit Indian number"
    )
    private String alternateMobileNumber;

    @PositiveOrZero(message = "Income amount cannot be negative")
    private Double incomeAmount;

    @NotBlank(message = "Branch code is required")
    @Pattern(regexp = "^[A-Z]{4}[0-9]{7}$", message = "Branch code must be a valid IFSC code (e.g., SBIN0001234)")
    private String branchCode;

    @NotBlank(message = "Address Line 1 is required")
    @Size(min = 5, max = 255, message = "Address Line 1 must be between 5 and 255 characters")
    private String addressLine1;

    @Size(max = 255, message = "Address Line 2 must not exceed 255 characters")
    private String addressLine2;

    @NotBlank(message = "City is required")
    @Size(min = 2, max = 100, message = "City must be between 2 and 100 characters")
    private String city;

    @NotBlank(message = "State is required")
    @Size(min = 2, max = 100, message = "State must be between 2 and 100 characters")
    private String state;

    @NotBlank(message = "Postal code is required")
    @Pattern(regexp = "^[0-9]{5,6}$", message = "Postal code must be 5 or 6 digits")
    private String postalCode;

    @NotBlank(message = "Country is required")
    @Size(min = 2, max = 100, message = "Country must be between 2 and 100 characters")
    private String country;

}


