package com.cts.identityservices.security;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "jwt")
@Getter
@Setter
public class JwtProperties {

    // Base64-encoded HMAC-SHA256 secret — must be ≥ 256 bits (44 Base64 chars minimum)
    // Set JWT_SECRET env var in production. Never commit the actual value to source control.
    private String secret;

    // Short-lived access token — validated statlessly by the gateway on every request
    // Default: 15 minutes. Keep short to limit the damage window if stolen.
    private long accessTokenExpirationMs = 900_000L;       // 15 min

    // Long-lived refresh token — opaque, stored hashed in DB, single-use (rotation)
    // Default: 7 days. Revoked on logout and on reuse detection.
    private long refreshTokenExpirationMs = 604_800_000L;  // 7 days
}
