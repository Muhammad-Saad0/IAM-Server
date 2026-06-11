package com.example.iam.auth.application.service;

import com.example.iam.account.domain.model.User;
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
        String rawToken = refreshTokenSecretService.generateRawToken();
        String tokenHash = refreshTokenSecretService.hashToken(rawToken);

        RefreshToken refreshToken = RefreshToken.issue(user, tokenHash, userAgent, issuedAt, expiresAt);
        refreshTokenPersistencePort.save(refreshToken);

        return new IssuedRefreshToken(rawToken, expiresAt);
    }

    public record IssuedRefreshToken(String token, Instant expiresAt) {
    }
}
