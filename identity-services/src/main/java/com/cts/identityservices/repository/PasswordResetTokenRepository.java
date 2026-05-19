package com.cts.identityservices.repository;

import com.cts.identityservices.entity.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    /** Latest unused row for an email — the OTP that's currently in play. */
    Optional<PasswordResetToken> findTopByEmailAndUsedFalseOrderByCreatedAtDesc(String email);
}
