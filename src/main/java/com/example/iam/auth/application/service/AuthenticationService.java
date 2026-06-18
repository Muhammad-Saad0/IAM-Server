package com.example.iam.auth.application.service;

import com.example.iam.account.application.port.out.UserPersistencePort;
import com.example.iam.account.application.port.out.UserRolePersistencePort;
import com.example.iam.account.domain.model.User;
import com.example.iam.auth.adapter.security.JwtTokenIssuer;
import com.example.iam.auth.application.exception.InvalidCredentialsException;
import com.example.iam.auth.application.exception.InvalidRefreshTokenException;
import com.example.iam.auth.application.port.out.AuthEventPersistencePort;
import com.example.iam.auth.application.port.out.PasswordVerifier;
import com.example.iam.auth.domain.model.AuthEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthenticationService {
    private final UserPersistencePort userPersistencePort;
    private final UserRolePersistencePort userRolePersistencePort;
    private final AuthEventPersistencePort authEventPersistencePort;
    private final PasswordVerifier passwordVerifier;
    private final JwtTokenIssuer jwtTokenIssuer;
    private final RefreshTokenService refreshTokenService;
    private final Clock clock;

    // Use cases
    @Transactional
    public AuthenticationResult login(String email, String password, String userAgent) {
        Instant now = clock.instant();
        User user = userPersistencePort.findByEmail(email)
                .orElseThrow(() -> invalidLogin(email, userAgent, now));

        if (!user.canAuthenticate() || !passwordVerifier.matches(password, user.getPasswordHash())) {
            throw invalidLogin(email, userAgent, now);
        }

        List<String> roleNames = userRolePersistencePort.findRoleNamesByUserId(user.getId());
        JwtTokenIssuer.IssuedAccessToken accessToken = jwtTokenIssuer.issueAccessToken(
                user.getId(),
                user.getEmail(),
                roleNames
        );
        RefreshTokenService.IssuedRefreshToken refreshToken = refreshTokenService.issueRefreshToken(user, userAgent);

        authEventPersistencePort.save(AuthEvent.loginSuccess(user, userAgent, now));

        return new AuthenticationResult(accessToken.token(), accessToken.expiresAt(), refreshToken.token(), refreshToken.expiresAt());
    }

    @Transactional(noRollbackFor = InvalidRefreshTokenException.class)
    public AuthenticationResult refreshAccessToken(String rawRefreshToken, String userAgent) {
        RefreshTokenService.RotatedRefreshToken rotatedRefreshToken = refreshTokenService.rotateRefreshToken(
                rawRefreshToken,
                userAgent
        );

        List<String> roleNames = userRolePersistencePort.findRoleNamesByUserId(rotatedRefreshToken.user().getId());
        JwtTokenIssuer.IssuedAccessToken accessToken = jwtTokenIssuer.issueAccessToken(
                rotatedRefreshToken.user().getId(),
                rotatedRefreshToken.user().getEmail(),
                roleNames
        );

        return new AuthenticationResult(
                accessToken.token(),
                accessToken.expiresAt(),
                rotatedRefreshToken.token(),
                rotatedRefreshToken.expiresAt()
        );
    }

    @Transactional
    public void logout(String rawRefreshToken) {
        refreshTokenService.revokeRefreshTokenFamily(rawRefreshToken);
        // TODO: Record a LOGOUT auth event once logout can resolve the user from the refresh token.
    }

    // Helpers
    private InvalidCredentialsException invalidLogin(String email, String userAgent, Instant now) {
        authEventPersistencePort.save(AuthEvent.loginFailure(email, userAgent, now));
        return new InvalidCredentialsException();
    }

    // Results
    public record AuthenticationResult(
            String accessToken,
            Instant accessTokenExpiresAt,
            String refreshToken,
            Instant refreshTokenExpiresAt
    ) {
    }
}
