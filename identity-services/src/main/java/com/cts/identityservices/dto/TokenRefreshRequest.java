package com.cts.identityservices.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "Refresh token request — exchange a valid refresh token for a new token pair")
public class TokenRefreshRequest {

    @NotBlank(message = "Refresh token is required")
    @Size(max = 512, message = "Refresh token must not exceed 512 characters")
    @Schema(description = "Opaque refresh token received from login or previous refresh")
    private String refreshToken;
}
