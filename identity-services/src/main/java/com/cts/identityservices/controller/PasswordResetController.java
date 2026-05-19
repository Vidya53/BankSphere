package com.cts.identityservices.controller;

import com.cts.identityservices.dto.ForgotPasswordRequest;
import com.cts.identityservices.dto.ResetPasswordRequest;
import com.cts.identityservices.dto.VerifyOtpRequest;
import com.cts.identityservices.dto.response.ApiResponse;
import com.cts.identityservices.service.PasswordResetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Public endpoints for the email-OTP password-reset flow. All three are
 * gateway-public — the user is, by definition, not signed in.
 *
 * Flow:
 *   POST /auth/forgot-password { email }
 *      → 202 Accepted when the email is registered.
 *      → 404 Not Found  when no account exists for that email,
 *        so the form can highlight the field and ask the user to retype.
 *
 *   POST /auth/verify-otp { email, otp }
 *      → 200 { resetToken }. Single-use, 15-minute lifetime.
 *
 *   POST /auth/reset-password { email, resetToken, newPassword }
 *      → 200 OK. The reset row is consumed.
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Password · Reset Flow",
     description = "Email-OTP-based password reset for users who cannot sign in")
public class PasswordResetController {

    private final PasswordResetService service;

    @PostMapping("/forgot-password")
    @Operation(
        summary = "Request a password-reset OTP by email",
        description = """
                Step 1 of the reset flow. Verifies the email belongs to a registered account, then
                emails a 6-digit OTP. Returns 404 when the address is not on file so the form can
                surface a clear "no account with this email" message.

                **Allowed roles:** Any authenticated user
                **Side effects:** Persists an OTP row; sends a reset email."""
    )
    public ResponseEntity<ApiResponse<Void>> forgot(@Valid @RequestBody ForgotPasswordRequest req) {
        service.requestOtp(req.getEmail());
        return ResponseEntity.accepted().body(ApiResponse.success(null,
                "A 6-digit code has been sent to your email."));
    }

    @PostMapping("/verify-otp")
    @Operation(
        summary = "Verify the OTP and obtain a one-time reset token",
        description = """
                Step 2 of the reset flow. Exchanges a valid OTP for a single-use reset token with a
                15-minute lifetime.

                **Allowed roles:** Any authenticated user
                **Side effects:** Marks the OTP row consumed; issues a reset token."""
    )
    public ResponseEntity<ApiResponse<Map<String, String>>> verifyOtp(
            @Valid @RequestBody VerifyOtpRequest req) {
        String resetToken = service.verifyOtp(req.getEmail(), req.getOtp());
        return ResponseEntity.ok(ApiResponse.success(
                Map.of("resetToken", resetToken),
                "OTP verified. You may now set a new password."));
    }

    @PostMapping("/reset-password")
    @Operation(
        summary = "Reset the password using a previously-verified reset token",
        description = """
                Step 3 of the reset flow. Consumes the reset token from /verify-otp and writes the new
                bcrypt password hash. The reset row is invalidated whether or not the call succeeds.

                **Allowed roles:** Any authenticated user
                **Side effects:** Updates the password hash; consumes the reset token row."""
    )
    public ResponseEntity<ApiResponse<Void>> reset(@Valid @RequestBody ResetPasswordRequest req) {
        service.resetPassword(req.getEmail(), req.getResetToken(), req.getNewPassword());
        return ResponseEntity.ok(ApiResponse.success(null,
                "Password updated. Please sign in with your new password."));
    }
}
