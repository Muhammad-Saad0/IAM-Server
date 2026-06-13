package com.example.iam.auth.adapter.in.web;

import com.example.iam.auth.application.exception.InvalidRefreshTokenException;
import com.example.iam.auth.application.service.AuthenticationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Clock;
import java.time.Duration;
import java.util.List;

@RestController
@RequestMapping("/auth")
public class AuthController {
    private static final String REFRESH_TOKEN_COOKIE = "refresh_token";
    private static final String USER_AGENT_HEADER = "User-Agent";
    private static final String EMAIL_CLAIM = "email";
    private static final String ROLES_CLAIM = "roles";

    private final AuthenticationService authenticationService;
    private final Clock clock;

    public AuthController(AuthenticationService authenticationService, Clock clock) {
        this.authenticationService = authenticationService;
        this.clock = clock;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest
    ) {
        AuthenticationService.AuthenticationResult result = authenticationService.login(
                request.email(),
                request.password(),
                httpRequest.getHeader(USER_AGENT_HEADER)
        );

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshTokenCookie(result.refreshToken(), result.refreshTokenExpiresAt()).toString())
                .body(new LoginResponse(result.accessToken(), result.accessTokenExpiresAt()));
    }

    @PostMapping("/refresh")
    public ResponseEntity<RefreshAccessTokenResponse> refreshAccessToken(
            @CookieValue(name = REFRESH_TOKEN_COOKIE, required = false) String rawRefreshToken,
            HttpServletRequest httpRequest
    ) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            throw new InvalidRefreshTokenException();
        }

        AuthenticationService.AuthenticationResult result = authenticationService.refreshAccessToken(
                rawRefreshToken,
                httpRequest.getHeader(USER_AGENT_HEADER)
        );

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshTokenCookie(result.refreshToken(), result.refreshTokenExpiresAt()).toString())
                .body(new RefreshAccessTokenResponse(result.accessToken(), result.accessTokenExpiresAt()));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @CookieValue(name = REFRESH_TOKEN_COOKIE, required = false) String rawRefreshToken
    ) {
        authenticationService.logout(rawRefreshToken);

        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, clearRefreshTokenCookie().toString())
                .build();
    }

    @GetMapping("/me")
    public CurrentUserResponse me(@AuthenticationPrincipal Jwt jwt) {
        List<String> roles = jwt.getClaimAsStringList(ROLES_CLAIM);

        return new CurrentUserResponse(
                jwt.getSubject(),
                jwt.getClaimAsString(EMAIL_CLAIM),
                roles == null ? List.of() : roles
        );
    }

    private ResponseCookie refreshTokenCookie(String refreshToken, java.time.Instant refreshTokenExpiresAt) {
        Duration maxAge = Duration.between(clock.instant(), refreshTokenExpiresAt);

        return ResponseCookie.from(REFRESH_TOKEN_COOKIE, refreshToken)
                .httpOnly(true)
                .secure(true)
                .sameSite("Lax")
                .path("/auth")
                .maxAge(maxAge)
                .build();
    }

    private ResponseCookie clearRefreshTokenCookie() {
        return ResponseCookie.from(REFRESH_TOKEN_COOKIE, "")
                .httpOnly(true)
                .secure(true)
                .sameSite("Lax")
                .path("/auth")
                .maxAge(Duration.ZERO)
                .build();
    }
}
