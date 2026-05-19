package com.cts.identityservices.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDate;

@Data
@Schema(description = "Signup request payload for registering a new user")
public class SignupRequest {

    @NotBlank(message = "Full name is required")
    @Size(min = 2, max = 100, message = "Full name must be between 2 and 100 characters")
    @Pattern(regexp = "^[A-Za-z][A-Za-z .'-]{1,99}$", message = "Full name may contain letters, spaces, hyphens and apostrophes only")
    @Schema(description = "User's full name", example = "Jane Doe")
    private String fullName;

    @NotBlank(message = "Email is required")
    @Email(message = "Please provide a valid email address")
    @Size(max = 254, message = "Email must not exceed 254 characters")
    @Schema(description = "User's email address", example = "jane.doe@banksphere.com")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 64, message = "Password must be between 8 and 64 characters")
    @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$", message = "Password must contain at least one letter and one digit")
    @Schema(description = "Password (min 8 characters, must contain at least one letter and one digit)", example = "Secret123")
    private String password;

    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^[6-9][0-9]{9}$", message = "Mobile must be a 10-digit number starting with 6, 7, 8 or 9")
    @Schema(description = "10-digit Indian mobile number", example = "9876543210")
    private String phoneNumber;

    @NotNull(message = "Date of birth is required")
    @Past(message = "Date of birth must be in the past")
    @Schema(description = "Date of birth (must be in the past)", example = "1995-04-12")
    private LocalDate dateOfBirth;
}
