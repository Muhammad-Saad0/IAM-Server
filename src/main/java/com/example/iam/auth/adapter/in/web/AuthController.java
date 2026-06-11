package com.example.iam.auth.adapter.in.web;

import com.example.iam.auth.application.exception.InvalidCredentialsException;
import com.example.iam.auth.application.service.LoginService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Clock;
import java.time.Duration;

@RestController
@RequestMapping("/auth")
public class AuthController {
    private static final String REFRESH_TOKEN_COOKIE = "refresh_token";
    private static final String USER_AGENT_HEADER = "User-Agent";

    private final LoginService loginService;
    private final Clock clock;

    public AuthController(LoginService loginService, Clock clock) {
        this.loginService = loginService;
        this.clock = clock;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest
    ) {
        try {
            LoginService.LoginResult result = loginService.login(
                    request.email(),
                    request.password(),
                    httpRequest.getHeader(USER_AGENT_HEADER)
            );

            return ResponseEntity.ok()
                    .header(HttpHeaders.SET_COOKIE, refreshTokenCookie(result).toString())
                    .body(new LoginResponse(result.accessToken(), result.accessTokenExpiresAt()));
        } catch (InvalidCredentialsException exception) {
            return ResponseEntity.status(401).build();
        }
    }

    private ResponseCookie refreshTokenCookie(LoginService.LoginResult result) {
        Duration maxAge = Duration.between(clock.instant(), result.refreshTokenExpiresAt());

        return ResponseCookie.from(REFRESH_TOKEN_COOKIE, result.refreshToken())
                .httpOnly(true)
                .secure(true)
                .sameSite("Lax")
                .path("/auth/refresh")
                .maxAge(maxAge)
                .build();
    }
}
