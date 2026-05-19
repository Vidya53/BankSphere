package com.cts.apigateway.filter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;

/**
 * Lightweight JWT utility for the API Gateway.
 *
 * The gateway validates access tokens on every inbound request.
 * Services never see the token — the gateway strips the Authorization header and
 * forwards user context as plain HTTP headers instead.
 *
 * We deliberately distinguish EXPIRED from INVALID so that the client
 * knows whether to refresh (expired) or to re-login (invalid/tampered).
 */
@Component
public class GatewayJwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    public enum TokenStatus { VALID, EXPIRED, INVALID }

    /**
     * Validates a raw JWT string and returns its status.
     * Never throws — all parsing exceptions are translated to a status code.
     */
    public TokenStatus validate(String token) {
        try {
            Jwts.parser().verifyWith(signingKey()).build().parseSignedClaims(token);
            return TokenStatus.VALID;
        } catch (ExpiredJwtException e) {
            return TokenStatus.EXPIRED;
        } catch (JwtException | IllegalArgumentException e) {
            return TokenStatus.INVALID;
        }
    }

    /**
     * Extracts all claims from a valid token.
     * Call only after {@link #validate} returns VALID.
     */
    public Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey signingKey() {
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
    }
}
