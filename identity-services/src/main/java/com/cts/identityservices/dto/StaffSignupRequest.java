package com.cts.identityservices.dto;

import com.cts.identityservices.entity.Role;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDate;

@Data
@Schema(description = "Request payload for admin-created staff user accounts")
public class StaffSignupRequest {

    @NotBlank(message = "Full name is required")
    @Size(min = 2, max = 100, message = "Full name must be between 2 and 100 characters")
    @Pattern(regexp = "^[A-Za-z][A-Za-z .'-]{1,99}$", message = "Full name may contain letters, spaces, hyphens and apostrophes only")
    private String fullName;

    @NotBlank(message = "Email is required")
    @Email(message = "Please provide a valid email address")
    @Size(max = 254, message = "Email must not exceed 254 characters")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 64, message = "Password must be between 8 and 64 characters")
    @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$", message = "Password must contain at least one letter and one digit")
    private String password;

    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^[6-9][0-9]{9}$", message = "Mobile must be a 10-digit number starting with 6, 7, 8 or 9")
    private String phoneNumber;

    @NotNull(message = "Date of birth is required")
    @Past(message = "Date of birth must be in the past")
    private LocalDate dateOfBirth;

    @NotNull(message = "Role is required")
    @Schema(description = "Staff role — must not be CUSTOMER", allowableValues = {"CSR", "LOAN_OFFICER", "BRANCH_MANAGER", "ADMIN"})
    private Role role;

    @Pattern(regexp = "^$|^[A-Z0-9]{2,20}$", message = "Branch code must be 2-20 uppercase letters or digits")
    @Schema(description = "Branch code — required for CSR, LOAN_OFFICER, BRANCH_MANAGER; optional for ADMIN")
    private String branchCode;
}
