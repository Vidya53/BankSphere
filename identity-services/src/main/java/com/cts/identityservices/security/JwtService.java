package com.cts.identityservices.security;

import com.cts.identityservices.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class JwtService {

    private final JwtProperties jwtProperties;

    /**
     * Generates a short-lived, stateless access token.
     *
     * Claims embedded:
     *   sub        — userId (String)
     *   email      — user email
     *   role       — role name (e.g. "BRANCH_MANAGER")
     *   branchCode — nullable; present for staff users only
     *   fullName   — display name for downstream services
     *
     * The gateway validates this token on every request and forwards the claims
     * as HTTP headers — services never see the raw token.
     */
    public String generateAccessToken(User user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("email",      user.getEmail());
        claims.put("role",       user.getRole().name());
        claims.put("fullName",   user.getFullName());
        claims.put("branchCode", user.getBranchCode());

        return Jwts.builder()
                .claims(claims)
                .subject(String.valueOf(user.getId()))
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + jwtProperties.getAccessTokenExpirationMs()))
                .signWith(signingKey())
                .compact();
    }

    public Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean isTokenValid(String token) {
        try {
            extractAllClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public long getAccessTokenExpirationMs() {
        return jwtProperties.getAccessTokenExpirationMs();
    }

    private SecretKey signingKey() {
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtProperties.getSecret()));
    }
}
