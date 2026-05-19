package com.cts.identityservices.service.impl;

import com.cts.identityservices.entity.RefreshToken;
import com.cts.identityservices.exception.RefreshTokenException;
import com.cts.identityservices.repository.RefreshTokenRepository;
import com.cts.identityservices.security.JwtProperties;
import com.cts.identityservices.service.RefreshTokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;

@Service
@RequiredArgsConstructor
@Slf4j
public class RefreshTokenServiceImpl implements RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtProperties jwtProperties;
    private final SecureRandom secureRandom = new SecureRandom();

    @Override
    @Transactional
    public String createRefreshToken(Long userId, String userAgent, String ipAddress) {
        String rawToken = generateSecureToken();
        String tokenHash = hashToken(rawToken);

        RefreshToken token = RefreshToken.builder()
                .userId(userId)
                .tokenHash(tokenHash)
                .expiresAt(LocalDateTime.now().plusSeconds(
                        jwtProperties.getRefreshTokenExpirationMs() / 1_000))
                .revoked(false)
                .userAgent(truncate(userAgent, 512))
                .ipAddress(truncate(ipAddress, 64))
                .build();

        refreshTokenRepository.save(token);
        log.debug("Refresh token created for userId={}", userId);
        return rawToken;
    }

    @Override
    @Transactional
    public Long verifyAndRotate(String rawToken) {
        String tokenHash = hashToken(rawToken);

        RefreshToken token = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(RefreshTokenException::notFound);

        // Reuse detection: presenting a revoked token means it was likely stolen.
        // Nuclear response: terminate all sessions for this user immediately.
        if (token.isRevoked()) {
            log.warn("SECURITY ALERT: Revoked refresh token presented for userId={}. Revoking all sessions.",
                    token.getUserId());
            refreshTokenRepository.revokeAllActiveByUserId(token.getUserId());
            throw RefreshTokenException.revokedWithSuspectedTheft();
        }

        if (token.getExpiresAt().isBefore(LocalDateTime.now())) {
            token.setRevoked(true);
            refreshTokenRepository.save(token);
            throw RefreshTokenException.expired();
        }

        // Single-use: revoke immediately before issuing the new token
        token.setRevoked(true);
        refreshTokenRepository.save(token);

        log.debug("Refresh token rotated for userId={}", token.getUserId());
        return token.getUserId();
    }

    @Override
    @Transactional
    public void revokeToken(String rawToken) {
        String tokenHash = hashToken(rawToken);
        refreshTokenRepository.findByTokenHash(tokenHash).ifPresent(t -> {
            if (!t.isRevoked()) {
                t.setRevoked(true);
                refreshTokenRepository.save(t);
                log.debug("Refresh token revoked for userId={}", t.getUserId());
            }
        });
    }

    @Override
    @Transactional
    public void revokeAllForUser(Long userId) {
        int count = refreshTokenRepository.revokeAllActiveByUserId(userId);
        log.info("Revoked {} active refresh tokens for userId={}", count, userId);
    }

    // Runs daily at 03:00 — removes tokens that are expired or revoked + old
    @Scheduled(cron = "0 0 3 * * ?")
    @Transactional
    public void cleanupStaleTokens() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime staleCutoff = now.minusDays(30);
        int deleted = refreshTokenRepository.deleteStaleTokens(now, staleCutoff);
        if (deleted > 0) {
            log.info("Cleaned up {} stale refresh tokens", deleted);
        }
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private String generateSecureToken() {
        byte[] bytes = new byte[32]; // 256 bits
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hashToken(String rawToken) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(64);
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 is mandated by the JVM spec — this can never happen
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }

    private String truncate(String value, int maxLength) {
        if (value == null) return null;
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }
}
