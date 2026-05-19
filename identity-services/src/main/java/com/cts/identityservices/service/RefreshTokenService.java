package com.cts.identityservices.service;

public interface RefreshTokenService {

    /**
     * Creates a new refresh token for the given user.
     * Stores a SHA-256 hash in the database and returns the raw token to be sent to the client.
     *
     * @param userId   owner user ID
     * @param userAgent optional User-Agent header for session analytics
     * @param ipAddress optional client IP for session analytics
     * @return raw (plain-text) refresh token — send to client, never log
     */
    String createRefreshToken(Long userId, String userAgent, String ipAddress);

    /**
     * Verifies a refresh token and atomically revokes it (single-use rotation).
     *
     * If the token is already revoked, ALL active tokens for the user are revoked
     * (suspected theft response). If expired or not found, an exception is thrown.
     *
     * @param rawToken raw token from the client
     * @return userId for the verified token
     * @throws com.cts.identityservices.exception.RefreshTokenException on invalid, expired, or reuse
     */
    Long verifyAndRotate(String rawToken);

    /**
     * Revokes a single refresh token (single-session logout).
     * Silently ignores unknown tokens (idempotent).
     */
    void revokeToken(String rawToken);

    /**
     * Revokes ALL active refresh tokens for a user (logout everywhere).
     */
    void revokeAllForUser(Long userId);
}
