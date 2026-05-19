package com.cts.identityservices.exception;

/**
 * Thrown for any refresh-token error: not found, expired, or revoked.
 * A single exception type is intentional — we don't want to leak whether
 * a token existed vs was revoked (security through ambiguity).
 */
public class RefreshTokenException extends RuntimeException {

    private final String errorCode;

    public RefreshTokenException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public static RefreshTokenException notFound() {
        return new RefreshTokenException("Invalid refresh token", "REFRESH_TOKEN_INVALID");
    }

    public static RefreshTokenException expired() {
        return new RefreshTokenException("Refresh token has expired — please log in again", "REFRESH_TOKEN_EXPIRED");
    }

    public static RefreshTokenException revokedWithSuspectedTheft() {
        return new RefreshTokenException(
                "Refresh token was already used — possible token theft detected. All sessions have been terminated.",
                "REFRESH_TOKEN_REUSE_DETECTED");
    }
}
