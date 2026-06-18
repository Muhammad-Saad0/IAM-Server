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
import java.util.List;
import java.util.UUID;

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
        PersistedRefreshToken issuedRefreshToken = persistRefreshToken(
                user,
                userAgent,
                UUID.randomUUID()
        );

        return new IssuedRefreshToken(issuedRefreshToken.token(), issuedRefreshToken.expiresAt());
    }

    private PersistedRefreshToken persistRefreshToken(
            User user,
            String userAgent,
            UUID tokenFamilyId
    ) {
        Instant issuedAt = clock.instant();
        Instant expiresAt = issuedAt.plus(refreshTokenTtl);
        String rawRefreshToken = refreshTokenSecretService.generateRawToken();
        String tokenHash = refreshTokenSecretService.hashToken(rawRefreshToken);

        RefreshToken refreshToken = RefreshToken.issueInFamily(
                user,
                tokenHash,
                tokenFamilyId,
                userAgent,
                issuedAt,
                expiresAt
        );
        RefreshToken savedRefreshToken = refreshTokenPersistencePort.save(refreshToken);

        return new PersistedRefreshToken(savedRefreshToken, rawRefreshToken, expiresAt);
    }

    @Transactional(noRollbackFor = InvalidRefreshTokenException.class)
    public RotatedRefreshToken rotateRefreshToken(String rawRefreshToken, String userAgent) {
        Instant now = clock.instant();
        String tokenHash = refreshTokenSecretService.hashToken(rawRefreshToken);
        RefreshToken locatedRefreshToken = refreshTokenPersistencePort.findByTokenHash(tokenHash)
                .orElseThrow(InvalidRefreshTokenException::new);
        List<RefreshToken> tokenFamily = refreshTokenPersistencePort.findAllByTokenFamilyIdForUpdate(
                locatedRefreshToken.getTokenFamilyId()
        );
        RefreshToken currentRefreshToken = tokenFamily.stream()
                .filter(refreshToken -> refreshToken.getTokenHash().equals(tokenHash))
                .findFirst()
                .orElseThrow(InvalidRefreshTokenException::new);

        if (currentRefreshToken.getReplacedByToken() != null) {
            revokeTokens(tokenFamily, now);
            throw new InvalidRefreshTokenException();
        }

        if (!currentRefreshToken.isActiveAt(now) || !currentRefreshToken.getUser().canAuthenticate()) {
            throw new InvalidRefreshTokenException();
        }

        PersistedRefreshToken replacement = persistRefreshToken(
                currentRefreshToken.getUser(),
                userAgent,
                currentRefreshToken.getTokenFamilyId()
        );
        currentRefreshToken.replaceWith(replacement.refreshToken(), now);
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
                .ifPresent(refreshToken -> revokeTokens(
                        refreshTokenPersistencePort.findAllByTokenFamilyIdForUpdate(refreshToken.getTokenFamilyId()),
                        now
                ));
    }

    private void revokeTokens(List<RefreshToken> tokenFamily, Instant now) {
        tokenFamily.forEach(tokenInFamily -> {
            if (tokenInFamily.getRevokedAt() == null) {
                tokenInFamily.revoke(now);
                refreshTokenPersistencePort.save(tokenInFamily);
            }
        });
    }

    public record IssuedRefreshToken(String token, Instant expiresAt) {
    }

    public record RotatedRefreshToken(User user, String token, Instant expiresAt) {
    }

    private record PersistedRefreshToken(RefreshToken refreshToken, String token, Instant expiresAt) {
    }
}
