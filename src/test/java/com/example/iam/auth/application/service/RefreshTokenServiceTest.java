package com.example.iam.auth.application.service;

import com.example.iam.account.domain.model.User;
import com.example.iam.auth.application.exception.InvalidRefreshTokenException;
import com.example.iam.auth.application.port.out.RefreshTokenPersistencePort;
import com.example.iam.auth.domain.model.RefreshToken;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RefreshTokenServiceTest {
    private static final Instant NOW = Instant.parse("2026-06-18T12:00:00Z");
    private static final Duration TOKEN_TTL = Duration.ofDays(14);

    private StubRefreshTokenSecretService refreshTokenSecretService;
    private FakeRefreshTokenPersistencePort refreshTokenPersistencePort;
    private RefreshTokenService refreshTokenService;
    private User user;

    @BeforeEach
    void setUp() {
        refreshTokenSecretService = new StubRefreshTokenSecretService();
        refreshTokenPersistencePort = new FakeRefreshTokenPersistencePort();
        refreshTokenService = new RefreshTokenService(
                refreshTokenSecretService,
                refreshTokenPersistencePort,
                TOKEN_TTL,
                Clock.fixed(NOW, ZoneOffset.UTC)
        );
        user = User.register("admin@example.com", "password-hash", NOW.minusSeconds(3600));
    }

    @Test
    void rotationKeepsReplacementInTheSameFamily() {
        RefreshToken currentToken = RefreshToken.issue(
                user,
                "current-hash",
                "original-agent",
                NOW.minusSeconds(600),
                NOW.plusSeconds(600)
        );

        refreshTokenSecretService.generatedToken = "replacement-raw";
        refreshTokenSecretService.hashes.put("current-raw", "current-hash");
        refreshTokenSecretService.hashes.put("replacement-raw", "replacement-hash");
        refreshTokenPersistencePort.locatedToken = currentToken;
        refreshTokenPersistencePort.tokenFamily = List.of(currentToken);

        RefreshTokenService.RotatedRefreshToken result = refreshTokenService.rotateRefreshToken(
                "current-raw",
                "replacement-agent"
        );

        RefreshToken replacementToken = refreshTokenPersistencePort.savedTokens.getFirst();

        assertThat(result.token()).isEqualTo("replacement-raw");
        assertThat(result.expiresAt()).isEqualTo(NOW.plus(TOKEN_TTL));
        assertThat(replacementToken.getTokenFamilyId()).isEqualTo(currentToken.getTokenFamilyId());
        assertThat(replacementToken.getTokenHash()).isEqualTo("replacement-hash");
        assertThat(currentToken.getReplacedByToken()).isSameAs(replacementToken);
        assertThat(currentToken.getRevokedAt()).isEqualTo(NOW);
        assertThat(refreshTokenPersistencePort.familyLockRequested).isTrue();
        assertThat(refreshTokenPersistencePort.savedTokens).containsExactly(replacementToken, currentToken);
    }

    @Test
    void replayingRotatedTokenRevokesItsActiveReplacement() {
        UUID familyId = UUID.randomUUID();
        RefreshToken replayedToken = RefreshToken.issueInFamily(
                user,
                "replayed-hash",
                familyId,
                "original-agent",
                NOW.minusSeconds(1200),
                NOW.plusSeconds(600)
        );
        RefreshToken activeReplacement = RefreshToken.issueInFamily(
                user,
                "replacement-hash",
                familyId,
                "replacement-agent",
                NOW.minusSeconds(600),
                NOW.plusSeconds(1200)
        );
        replayedToken.replaceWith(activeReplacement, NOW.minusSeconds(600));

        refreshTokenSecretService.hashes.put("replayed-raw", "replayed-hash");
        refreshTokenPersistencePort.locatedToken = replayedToken;
        refreshTokenPersistencePort.tokenFamily = List.of(replayedToken, activeReplacement);

        assertThatThrownBy(() -> refreshTokenService.rotateRefreshToken("replayed-raw", "attacker-agent"))
                .isInstanceOf(InvalidRefreshTokenException.class);

        assertThat(activeReplacement.getRevokedAt()).isEqualTo(NOW);
        assertThat(refreshTokenPersistencePort.savedTokens).containsExactly(activeReplacement);
        assertThat(refreshTokenSecretService.generateTokenCalled).isFalse();
        assertThat(refreshTokenPersistencePort.familyLockRequested).isTrue();
    }

    private static final class StubRefreshTokenSecretService extends RefreshTokenSecretService {
        private final Map<String, String> hashes = new HashMap<>();
        private String generatedToken;
        private boolean generateTokenCalled;

        @Override
        public String generateRawToken() {
            generateTokenCalled = true;
            return generatedToken;
        }

        @Override
        public String hashToken(String rawToken) {
            return hashes.get(rawToken);
        }
    }

    private static final class FakeRefreshTokenPersistencePort implements RefreshTokenPersistencePort {
        private final List<RefreshToken> savedTokens = new ArrayList<>();
        private RefreshToken locatedToken;
        private List<RefreshToken> tokenFamily = List.of();
        private boolean familyLockRequested;

        @Override
        public RefreshToken save(RefreshToken refreshToken) {
            savedTokens.add(refreshToken);
            return refreshToken;
        }

        @Override
        public Optional<RefreshToken> findByTokenHash(String tokenHash) {
            return Optional.ofNullable(locatedToken);
        }

        @Override
        public List<RefreshToken> findAllByTokenFamilyId(UUID tokenFamilyId) {
            return tokenFamily;
        }

        @Override
        public List<RefreshToken> findAllByTokenFamilyIdForUpdate(UUID tokenFamilyId) {
            familyLockRequested = true;
            return tokenFamily;
        }
    }
}
