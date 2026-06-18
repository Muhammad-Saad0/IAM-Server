package com.example.iam.auth.domain.model;

import com.example.iam.account.domain.model.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "refresh_tokens")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RefreshToken {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "token_hash", nullable = false, unique = true)
    private String tokenHash;

    @Column(name = "token_family_id", nullable = false)
    private UUID tokenFamilyId;

    @Column(name = "user_agent", length = 512)
    private String userAgent;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "replaced_by_token_id")
    private RefreshToken replacedByToken;

    public RefreshToken(
            User user,
            String tokenHash,
            UUID tokenFamilyId,
            String userAgent,
            Instant createdAt,
            Instant expiresAt
    ) {
        this.user = Objects.requireNonNull(user, "user must not be null");
        this.tokenHash = Objects.requireNonNull(tokenHash, "tokenHash must not be null");
        this.tokenFamilyId = Objects.requireNonNull(tokenFamilyId, "tokenFamilyId must not be null");
        this.userAgent = userAgent;
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
        this.expiresAt = Objects.requireNonNull(expiresAt, "expiresAt must not be null");
    }

    public static RefreshToken issue(User user, String tokenHash, String userAgent, Instant now, Instant expiresAt) {
        return issueInFamily(user, tokenHash, UUID.randomUUID(), userAgent, now, expiresAt);
    }

    public static RefreshToken issueInFamily(
            User user,
            String tokenHash,
            UUID tokenFamilyId,
            String userAgent,
            Instant now,
            Instant expiresAt
    ) {
        return new RefreshToken(user, tokenHash, tokenFamilyId, userAgent, now, expiresAt);
    }

    public boolean isActiveAt(Instant now) {
        Objects.requireNonNull(now, "now must not be null");
        return revokedAt == null && expiresAt.isAfter(now);
    }

    public void revoke(Instant now) {
        this.revokedAt = Objects.requireNonNull(now, "now must not be null");
    }

    public void replaceWith(RefreshToken replacement, Instant now) {
        this.replacedByToken = Objects.requireNonNull(replacement, "replacement must not be null");
        revoke(now);
    }
}
