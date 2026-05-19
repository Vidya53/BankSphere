package com.cts.identityservices.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Data;

@Data
@Schema(description = "Login request payload")
public class LoginRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "Please provide a valid email address")
    @Size(max = 254, message = "Email must not exceed 254 characters")
    @Schema(description = "Registered email address", example = "user@banksphere.com")
    private String email;

    @NotBlank(message = "Password is required")
    @Schema(description = "Account password", example = "secret123")
    private String password;
}
