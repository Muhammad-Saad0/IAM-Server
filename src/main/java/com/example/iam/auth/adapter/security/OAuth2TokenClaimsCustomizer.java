package com.example.iam.auth.adapter.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.oidc.OidcScopes;
import org.springframework.security.oauth2.core.oidc.endpoint.OidcParameterNames;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class OAuth2TokenClaimsCustomizer implements OAuth2TokenCustomizer<JwtEncodingContext> {
    private static final String ROLE_PREFIX = "ROLE_";
    private static final String ROLES_CLAIM = "roles";
    private static final String EMAIL_CLAIM = "email";

    @Override
    public void customize(JwtEncodingContext context) {
        if (!isAccessToken(context) && !isIdToken(context)) {
            return;
        }

        context.getClaims().claim(ROLES_CLAIM, roles(context));

        if (isIdToken(context) && context.getAuthorizedScopes().contains(OidcScopes.EMAIL)) {
            context.getClaims().claim(EMAIL_CLAIM, context.getPrincipal().getName());
        }
    }

    private boolean isAccessToken(JwtEncodingContext context) {
        return OAuth2TokenType.ACCESS_TOKEN.equals(context.getTokenType());
    }

    private boolean isIdToken(JwtEncodingContext context) {
        return OidcParameterNames.ID_TOKEN.equals(context.getTokenType().getValue());
    }

    private List<String> roles(JwtEncodingContext context) {
        return context.getPrincipal().getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(authority -> authority.startsWith(ROLE_PREFIX))
                .map(authority -> authority.substring(ROLE_PREFIX.length()))
                .distinct()
                .sorted()
                .toList();
    }
}
