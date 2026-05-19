package com.cts.identityservices.service;

import com.cts.identityservices.entity.PasswordResetToken;
import com.cts.identityservices.entity.User;
import com.cts.identityservices.exception.InvalidCredentialsException;
import com.cts.identityservices.exception.UserNotFoundException;
import com.cts.identityservices.repository.PasswordResetTokenRepository;
import com.cts.identityservices.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.UUID;

/**
 * Three-step password reset:
 *
 *   requestOtp  — generate 6-digit OTP, hash it, persist with 10-min expiry,
 *                 email it to the registered user. Throws UserNotFoundException
 *                 (→ 404) when the email is not on file so the customer is told
 *                 to correct it. (We deliberately trade the account-enumeration
 *                 protection for the friendlier real-bank UX requested by the
 *                 product.)
 *
 *   verifyOtp   — match the OTP against the latest unused row. On success,
 *                 mint a single-use reset token (UUID), hash & persist it, and
 *                 return the raw token to the caller. Wrong OTPs increment the
 *                 attempts counter and kill the row after MAX_ATTEMPTS.
 *
 *   resetPassword — accept (email, resetToken, newPassword), validate the
 *                   token hash + expiry, update the user's password, mark the
 *                   row consumed.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PasswordResetService {

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final PasswordResetMailService mailService;

    private static final SecureRandom RANDOM = new SecureRandom();

    @Value("${password-reset.otp-expiry-minutes:10}")
    private int otpExpiryMinutes;

    @Value("${password-reset.token-expiry-minutes:15}")
    private int tokenExpiryMinutes;

    @Value("${password-reset.max-attempts:5}")
    private int maxAttempts;

    @Transactional
    public void requestOtp(String email) {
        // No account → friendly 404 so the form can highlight the email field.
        // (The account-enumeration risk this opens up is acceptable here: the
        // product wants users to know when they've mistyped their address.)
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException(
                        "No account is registered with " + email
                                + ". Please check the email and try again."));

        String otp = generateOtp();
        PasswordResetToken token = PasswordResetToken.builder()
                .email(user.getEmail())
                .otpHash(passwordEncoder.encode(otp))
                .otpExpiresAt(LocalDateTime.now().plusMinutes(otpExpiryMinutes))
                .attempts(0)
                .otpVerified(false)
                .used(false)
                .build();
        tokenRepository.save(token);

        mailService.sendOtp(user.getEmail(), user.getFullName(), otp);
    }

    @Transactional
    public String verifyOtp(String email, String otp) {
        PasswordResetToken token = tokenRepository
                .findTopByEmailAndUsedFalseOrderByCreatedAtDesc(email)
                .orElseThrow(() -> new InvalidCredentialsException(
                        "No active password-reset request. Please request a new OTP."));

        if (token.getOtpExpiresAt().isBefore(LocalDateTime.now())) {
            token.setUsed(true);
            tokenRepository.save(token);
            throw new InvalidCredentialsException("OTP has expired. Please request a new one.");
        }
        if (token.getAttempts() >= maxAttempts) {
            token.setUsed(true);
            tokenRepository.save(token);
            throw new InvalidCredentialsException("Too many incorrect attempts. Request a new OTP.");
        }
        if (!passwordEncoder.matches(otp, token.getOtpHash())) {
            token.setAttempts(token.getAttempts() + 1);
            tokenRepository.save(token);
            int remaining = Math.max(0, maxAttempts - token.getAttempts());
            throw new InvalidCredentialsException(
                    "Incorrect OTP. " + remaining + " attempt(s) remaining.");
        }

        // ── OTP verified — mint a single-use reset token ──────────────────
        String resetToken = randomToken();
        token.setOtpVerified(true);
        token.setResetTokenHash(passwordEncoder.encode(resetToken));
        token.setResetTokenExpiresAt(LocalDateTime.now().plusMinutes(tokenExpiryMinutes));
        tokenRepository.save(token);

        return resetToken;
    }

    @Transactional
    public void resetPassword(String email, String rawResetToken, String newPassword) {
        PasswordResetToken token = tokenRepository
                .findTopByEmailAndUsedFalseOrderByCreatedAtDesc(email)
                .orElseThrow(() -> new InvalidCredentialsException(
                        "Reset session not found. Start again from forgot-password."));

        if (!Boolean.TRUE.equals(token.getOtpVerified()) || token.getResetTokenHash() == null) {
            throw new InvalidCredentialsException("OTP was not verified. Verify the OTP first.");
        }
        if (token.getResetTokenExpiresAt() == null
                || token.getResetTokenExpiresAt().isBefore(LocalDateTime.now())) {
            token.setUsed(true);
            tokenRepository.save(token);
            throw new InvalidCredentialsException("Reset session expired. Start again.");
        }
        if (!passwordEncoder.matches(rawResetToken, token.getResetTokenHash())) {
            throw new InvalidCredentialsException("Invalid reset token. Start again.");
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new InvalidCredentialsException("Account no longer exists."));
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        token.setUsed(true);
        tokenRepository.save(token);
        log.info("Password reset completed for {}", email);
    }

    private static String generateOtp() {
        int n = RANDOM.nextInt(1_000_000);
        return String.format("%06d", n);
    }

    private static String randomToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
                + UUID.randomUUID().toString().replace("-", "");
    }
}
