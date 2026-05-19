package com.cts.identityservices.repository;

import com.cts.identityservices.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    @Modifying
    @Query("UPDATE RefreshToken r SET r.revoked = true WHERE r.userId = :userId AND r.revoked = false")
    int revokeAllActiveByUserId(@Param("userId") Long userId);

    // Periodic cleanup — delete tokens that have been expired AND revoked for more than 30 days
    @Modifying
    @Query("DELETE FROM RefreshToken r WHERE r.expiresAt < :cutoff OR r.revoked = true AND r.createdAt < :staleCutoff")
    int deleteStaleTokens(@Param("cutoff") LocalDateTime cutoff,
                          @Param("staleCutoff") LocalDateTime staleCutoff);
}
