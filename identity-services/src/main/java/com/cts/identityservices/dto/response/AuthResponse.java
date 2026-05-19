package com.cts.identityservices.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AuthResponse {

    private Long userId;
    private String email;
    private String fullName;
    private String role;
    private String branchCode;

    // Short-lived JWT — include in Authorization: Bearer <accessToken> header for every API call
    private String accessToken;

    // Long-lived opaque token — use only at POST /auth/refresh when the access token expires
    // Store securely (httpOnly cookie recommended for browser clients)
    private String refreshToken;

    @Builder.Default
    private String tokenType = "Bearer";

    // Access token lifetime in milliseconds — use this to schedule proactive refresh
    private long accessTokenExpiresIn;
}
