package com.example.iam.auth.application.service;

import com.example.iam.account.domain.model.User;
import com.example.iam.auth.application.exception.InvalidRefreshTokenException;
import com.example.iam.auth.application.port.out.RefreshTokenPersistencePort;
import com.example.iam.auth.domain.model.RefreshToken;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

@Service
public class RefreshTokenService {
    private final RefreshTokenSecretService refreshTokenSecretService;
    private final RefreshTokenPersistencePort refreshTokenPersistencePort;
    private final Duration refreshTokenTtl;
    private final Clock clock;

    public RefreshTokenService(
            RefreshTokenSecretService refreshTokenSecretService,
            RefreshTokenPersistencePort refreshTokenPersistencePort,
            @Value("${app.security.refresh-token-ttl}") Duration refreshTokenTtl,
            Clock clock
    ) {
        this.refreshTokenSecretService = refreshTokenSecretService;
        this.refreshTokenPersistencePort = refreshTokenPersistencePort;
        this.refreshTokenTtl = refreshTokenTtl;
        this.clock = clock;
    }

    @Transactional
    public IssuedRefreshToken issueRefreshToken(User user, String userAgent) {
        Instant issuedAt = clock.instant();
        Instant expiresAt = issuedAt.plus(refreshTokenTtl);
        String rawRefreshToken = refreshTokenSecretService.generateRawToken();
        String tokenHash = refreshTokenSecretService.hashToken(rawRefreshToken);

        RefreshToken refreshToken = RefreshToken.issue(user, tokenHash, userAgent, issuedAt, expiresAt);
        refreshTokenPersistencePort.save(refreshToken);

        return new IssuedRefreshToken(rawRefreshToken, expiresAt);
    }

    @Transactional
    public RotatedRefreshToken rotateRefreshToken(String rawRefreshToken, String userAgent) {
        Instant now = clock.instant();
        String tokenHash = refreshTokenSecretService.hashToken(rawRefreshToken);
        RefreshToken currentRefreshToken = refreshTokenPersistencePort.findByTokenHash(tokenHash)
                .orElseThrow(InvalidRefreshTokenException::new);

        if (!currentRefreshToken.isActiveAt(now) || !currentRefreshToken.getUser().canAuthenticate()) {
            throw new InvalidRefreshTokenException();
        }

        IssuedRefreshToken replacement = issueRefreshToken(currentRefreshToken.getUser(), userAgent);

        /*
         * issueRefreshToken returns only the raw token value and expiry because
         * callers need the cookie value, not the persistence entity. Rotation
         * needs the saved replacement entity so the old token can point at it.
         */
        RefreshToken replacementRefreshToken = refreshTokenPersistencePort.findByTokenHash(
                refreshTokenSecretService.hashToken(replacement.token())
        ).orElseThrow(InvalidRefreshTokenException::new);

        currentRefreshToken.replaceWith(replacementRefreshToken, now);
        refreshTokenPersistencePort.save(currentRefreshToken);

        return new RotatedRefreshToken(currentRefreshToken.getUser(), replacement.token(), replacement.expiresAt());
    }

    @Transactional
    public void revokeRefreshTokenFamily(String rawRefreshToken) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            return;
        }

        Instant now = clock.instant();
        String tokenHash = refreshTokenSecretService.hashToken(rawRefreshToken);

        refreshTokenPersistencePort.findByTokenHash(tokenHash)
                .ifPresent(refreshToken -> refreshTokenPersistencePort.findAllByTokenFamilyId(refreshToken.getTokenFamilyId())
                        .forEach(tokenInFamily -> {
                            if (tokenInFamily.getRevokedAt() == null) {
                                tokenInFamily.revoke(now);
                                refreshTokenPersistencePort.save(tokenInFamily);
                            }
                        }));
    }

    public record IssuedRefreshToken(String token, Instant expiresAt) {
    }

    public record RotatedRefreshToken(User user, String token, Instant expiresAt) {
    }
}
