package com.cts.identityservices.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ResetPasswordRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "Please provide a valid email address")
    @Size(max = 254, message = "Email must not exceed 254 characters")
    private String email;

    /** The single-use reset token returned from /auth/verify-otp. */
    @NotBlank(message = "Reset token is required")
    @Size(max = 512, message = "Reset token must not exceed 512 characters")
    private String resetToken;

    @NotBlank(message = "New password is required")
    @Size(min = 8, max = 64, message = "Password must be between 8 and 64 characters")
    @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$", message = "Password must contain at least one letter and one digit")
    private String newPassword;
}
