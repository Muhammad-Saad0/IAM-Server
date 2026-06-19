package com.example.iam.auth.adapter.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.oidc.endpoint.OidcParameterNames;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class OAuth2TokenClaimsCustomizerTest {
    private final OAuth2TokenClaimsCustomizer customizer = new OAuth2TokenClaimsCustomizer();

    @Test
    void accessTokenContainsPrincipalRoles() {
        JwtEncodingContext context = context(OAuth2TokenType.ACCESS_TOKEN, Set.of("openid", "iam.manage"));

        customizer.customize(context);

        assertThat(claims(context).get("roles"))
                .asList()
                .containsExactlyInAnyOrder("ADMIN", "USER");
    }

    @Test
    void idTokenContainsRolesAndScopedEmail() {
        JwtEncodingContext context = context(
                new OAuth2TokenType(OidcParameterNames.ID_TOKEN),
                Set.of("openid", "email")
        );

        customizer.customize(context);

        assertThat(claims(context))
                .containsEntry("email", "Admin@Example.COM");
        assertThat(claims(context).get("roles"))
                .asList()
                .containsExactlyInAnyOrder("ADMIN", "USER");
    }

    @Test
    void idTokenOmitsEmailWhenEmailScopeWasNotAuthorized() {
        JwtEncodingContext context = context(
                new OAuth2TokenType(OidcParameterNames.ID_TOKEN),
                Set.of("openid")
        );

        customizer.customize(context);

        assertThat(claims(context)).doesNotContainKey("email");
    }

    private JwtEncodingContext context(OAuth2TokenType tokenType, Set<String> scopes) {
        return JwtEncodingContext.with(
                        JwsHeader.with(SignatureAlgorithm.RS256),
                        JwtClaimsSet.builder()
                )
                .principal(new UsernamePasswordAuthenticationToken(
                        "Admin@Example.COM",
                        null,
                        List.of(
                                new SimpleGrantedAuthority("ROLE_ADMIN"),
                                new SimpleGrantedAuthority("ROLE_USER"),
                                new SimpleGrantedAuthority("SCOPE_openid")
                        )
                ))
                .authorizedScopes(scopes)
                .tokenType(tokenType)
                .build();
    }

    private Map<String, Object> claims(JwtEncodingContext context) {
        return context.getClaims().build().getClaims();
    }
}
