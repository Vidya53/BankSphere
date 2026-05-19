package com.cts.identityservices.controller;

import com.cts.identityservices.dto.LoginRequest;
import com.cts.identityservices.dto.SignupRequest;
import com.cts.identityservices.dto.TokenRefreshRequest;
import com.cts.identityservices.dto.response.ApiResponse;
import com.cts.identityservices.dto.response.AuthResponse;
import com.cts.identityservices.dto.response.TokenRefreshResponse;
import com.cts.identityservices.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Signup, login, token refresh, and logout for all users")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/signup")
    @Operation(
        summary = "Register a new customer account",
        description = """
                Creates a new CUSTOMER user and returns an access + refresh token pair on success.
                Staff accounts must instead be created by an ADMIN via POST /api/v1/admin/staff.

                **Allowed roles:** Any authenticated user
                **Side effects:** Persists a new user row; issues a refresh token."""
    )
    public ResponseEntity<ApiResponse<AuthResponse>> signup(
            @Valid @RequestBody SignupRequest request) {
        AuthResponse response = authService.signup(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(response, "User registered successfully"));
    }

    @PostMapping("/login")
    @Operation(
        summary = "Authenticate and receive a token pair",
        description = """
                Verifies credentials and returns a short-lived access token (15 min) and a long-lived
                refresh token (7 days). Captures user-agent and IP for session tracking.

                **Allowed roles:** Any authenticated user
                **Side effects:** Persists a refresh-token row tied to user-agent / IP."""
    )
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest) {
        String userAgent = httpRequest.getHeader("User-Agent");
        String ipAddress = extractClientIp(httpRequest);
        AuthResponse response = authService.login(request, userAgent, ipAddress);
        return ResponseEntity.ok(ApiResponse.success(response, "Login successful"));
    }

    @PostMapping("/refresh")
    @Operation(
        summary = "Rotate token pair using a refresh token",
        description = """
                Exchanges a valid refresh token for a new access token and a new refresh token. The submitted
                refresh token is immediately revoked (single-use rotation). Reusing a revoked refresh token
                terminates all sessions for that user as a security measure.

                **Allowed roles:** Any authenticated user
                **Side effects:** Revokes the submitted refresh token; issues a new refresh token."""
    )
    public ResponseEntity<ApiResponse<TokenRefreshResponse>> refresh(
            @Valid @RequestBody TokenRefreshRequest request,
            HttpServletRequest httpRequest) {
        String userAgent = httpRequest.getHeader("User-Agent");
        String ipAddress = extractClientIp(httpRequest);
        TokenRefreshResponse response = authService.refresh(request.getRefreshToken(), userAgent, ipAddress);
        return ResponseEntity.ok(ApiResponse.success(response, "Token refreshed successfully"));
    }

    @PostMapping("/logout")
    @Operation(
        summary = "Revoke the current session's refresh token",
        description = """
                Invalidates the provided refresh token. The access token remains valid until its
                natural ~15-minute expiry — an accepted trade-off for stateless JWT auth without
                a Redis blacklist.

                **Allowed roles:** Any authenticated user
                **Side effects:** Marks the refresh-token row as revoked."""
    )
    public ResponseEntity<ApiResponse<Void>> logout(
            @Valid @RequestBody TokenRefreshRequest request) {
        authService.logout(request.getRefreshToken());
        return ResponseEntity.ok(ApiResponse.success(null, "Logged out successfully"));
    }

    @PostMapping("/logout-all")
    @Operation(
        summary = "Revoke all sessions for the authenticated user",
        description = """
                Terminates every active session across all devices for the caller. Requires the
                X-User-Id header which is injected by the API gateway after JWT validation.

                **Allowed roles:** Any authenticated user
                **Side effects:** Revokes all refresh-token rows for the user."""
    )
    public ResponseEntity<ApiResponse<Void>> logoutAll(
            @RequestHeader("X-User-Id") Long userId) {
        authService.logoutAll(userId);
        return ResponseEntity.ok(ApiResponse.success(null, "All sessions terminated"));
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private String extractClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
