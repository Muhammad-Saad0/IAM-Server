package com.example.iam.auth.adapter.security;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.UUID;

@Component
public class JwtTokenIssuer {
    private static final String EMAIL_CLAIM = "email";
    private static final String ROLES_CLAIM = "roles";

    private final JwtEncoder jwtEncoder;
    private final Duration accessTokenTtl;
    private final Clock clock;

    public JwtTokenIssuer(
            @Qualifier("adminJwtEncoder") JwtEncoder jwtEncoder,
            @Value("${app.security.jwt.access-token-ttl}") Duration accessTokenTtl,
            Clock clock
    ) {
        this.jwtEncoder = jwtEncoder;
        this.accessTokenTtl = accessTokenTtl;
        this.clock = clock;
    }

    public IssuedAccessToken issueAccessToken(UUID userId, String email, Collection<String> roles) {
        Instant issuedAt = clock.instant();
        Instant expiresAt = issuedAt.plus(accessTokenTtl);

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject(userId.toString())
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .id(UUID.randomUUID().toString())
                .claim(EMAIL_CLAIM, email)
                .claim(ROLES_CLAIM, roles)
                .build();

        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        String token = jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();

        return new IssuedAccessToken(token, expiresAt);
    }

    public record IssuedAccessToken(String token, Instant expiresAt) {
    }
}
