package com.cts.identityservices.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * A single password-reset attempt. Lifecycle:
 *
 *   1. /auth/forgot-password generates a row with a hashed OTP (10-min expiry).
 *   2. /auth/verify-otp marks it `otpVerified=true` and stamps a `resetToken`
 *      hash that expires 15 minutes later — this is the only way to set a
 *      new password.
 *   3. /auth/reset-password consumes the row (`used=true`) and updates the
 *      user's password.
 *
 * Rows are not deleted on use; we keep them for audit. A scheduled cleanup
 * task can purge expired+used rows older than N days if needed.
 */
@Entity
@Table(name = "password_reset_tokens", indexes = {
        @Index(name = "idx_prt_email", columnList = "email"),
        @Index(name = "idx_prt_expiry", columnList = "otp_expires_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PasswordResetToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String email;

    /** BCrypt hash of the 6-digit OTP. */
    @Column(name = "otp_hash", nullable = false, length = 100)
    private String otpHash;

    @Column(name = "otp_expires_at", nullable = false)
    private LocalDateTime otpExpiresAt;

    /** Set true after /auth/verify-otp succeeds. */
    @Column(nullable = false)
    @Builder.Default
    private Boolean otpVerified = false;

    /** BCrypt hash of a random reset-token issued after OTP verification. */
    @Column(name = "reset_token_hash", length = 100)
    private String resetTokenHash;

    @Column(name = "reset_token_expires_at")
    private LocalDateTime resetTokenExpiresAt;

    /** Wrong-OTP counter — kills the row after MAX_ATTEMPTS. */
    @Column(nullable = false)
    @Builder.Default
    private Integer attempts = 0;

    /** Set true once /auth/reset-password consumes the row. */
    @Column(nullable = false)
    @Builder.Default
    private Boolean used = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
