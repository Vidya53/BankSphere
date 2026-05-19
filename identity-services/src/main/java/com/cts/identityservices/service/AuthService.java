package com.cts.identityservices.service;

import com.cts.identityservices.dto.LoginRequest;
import com.cts.identityservices.dto.SignupRequest;
import com.cts.identityservices.dto.StaffSignupRequest;
import com.cts.identityservices.dto.response.AuthResponse;
import com.cts.identityservices.dto.response.TokenRefreshResponse;

public interface AuthService {

    AuthResponse signup(SignupRequest request);

    AuthResponse login(LoginRequest request, String userAgent, String ipAddress);

    AuthResponse createStaffUser(StaffSignupRequest request);

    /**
     * Exchanges a valid refresh token for a new access token + rotated refresh token.
     * The old refresh token is revoked immediately (single-use rotation).
     */
    TokenRefreshResponse refresh(String rawRefreshToken, String userAgent, String ipAddress);

    /**
     * Revokes the given refresh token (single-session logout).
     * The associated access token will continue to work until its natural expiry (~15 min).
     */
    void logout(String rawRefreshToken);

    /**
     * Revokes all refresh tokens for a user (logout from all devices).
     */
    void logoutAll(Long userId);
}
