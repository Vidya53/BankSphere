package com.cts.identityservices.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Persisted refresh token.
 *
 * The actual token sent to the client is a cryptographically-random 32-byte value
 * encoded as URL-safe Base64. We store only its SHA-256 hash — the plain token
 * never touches the database, so a compromised DB cannot be used to hijack sessions.
 *
 * Every refresh token is single-use (rotation): when a client presents it, the record
 * is immediately marked revoked and a new token pair is issued. Presenting a revoked
 * token indicates possible theft, so we revoke ALL tokens for that user.
 */
@Entity
@Table(
        name = "refresh_tokens",
        indexes = {
                @Index(name = "idx_rt_token_hash", columnList = "tokenHash"),
                @Index(name = "idx_rt_user_id",   columnList = "userId")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    // SHA-256 hex digest of the raw token — never store the plain token
    @Column(nullable = false, unique = true, length = 64)
    private String tokenHash;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    @Column(nullable = false)
    @Builder.Default
    private boolean revoked = false;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    // For security analytics — helps detect unusual sessions
    @Column(length = 512)
    private String userAgent;

    @Column(length = 64)
    private String ipAddress;
}
