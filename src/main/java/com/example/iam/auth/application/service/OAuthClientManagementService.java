package com.example.iam.auth.application.service;

import com.example.iam.auth.application.exception.ManagementValidationException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.oidc.OidcScopes;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.Clock;
import java.time.Instant;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OAuthClientManagementService {
    private static final Set<String> ALLOWED_SCOPES = Set.of(OidcScopes.OPENID, OidcScopes.EMAIL);

    private final RegisteredClientRepository registeredClientRepository;
    private final Clock clock;

    @Transactional
    public CreatedOAuthClient createClient(
            String clientName,
            List<String> requestedRedirectUris,
            List<String> requestedScopes
    ) {
        List<String> redirectUris = validatedRedirectUris(requestedRedirectUris);
        List<String> scopes = effectiveScopes(requestedScopes);
        Instant issuedAt = clock.instant();
        String clientId = UUID.randomUUID().toString();

        RegisteredClient.Builder builder = RegisteredClient.withId(UUID.randomUUID().toString())
                .clientId(clientId)
                .clientIdIssuedAt(issuedAt)
                .clientName(clientName.trim())
                .clientAuthenticationMethod(ClientAuthenticationMethod.NONE)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .clientSettings(ClientSettings.builder()
                        .requireProofKey(true)
                        .requireAuthorizationConsent(false)
                        .build());
        redirectUris.forEach(builder::redirectUri);
        scopes.forEach(builder::scope);

        RegisteredClient registeredClient = builder.build();
        registeredClientRepository.save(registeredClient);

        return new CreatedOAuthClient(
                registeredClient.getClientId(),
                registeredClient.getClientName(),
                registeredClient.getClientIdIssuedAt(),
                redirectUris,
                scopes
        );
    }

    private List<String> validatedRedirectUris(List<String> requestedRedirectUris) {
        if (requestedRedirectUris == null || requestedRedirectUris.isEmpty()) {
            throw new ManagementValidationException("At least one redirect URI is required");
        }

        LinkedHashSet<String> uniqueRedirectUris = new LinkedHashSet<>();
        for (String redirectUri : requestedRedirectUris) {
            validateRedirectUri(redirectUri);
            if (!uniqueRedirectUris.add(redirectUri)) {
                throw new ManagementValidationException("Redirect URIs must be unique");
            }
        }
        return List.copyOf(uniqueRedirectUris);
    }

    private void validateRedirectUri(String redirectUri) {
        try {
            URI uri = new URI(redirectUri);
            String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
            if (!uri.isAbsolute()
                    || (!"http".equals(scheme) && !"https".equals(scheme))
                    || uri.getHost() == null
                    || uri.getFragment() != null) {
                throw new ManagementValidationException("Redirect URI must be an absolute HTTP(S) URI without a fragment");
            }
        } catch (URISyntaxException | NullPointerException exception) {
            throw new ManagementValidationException("Redirect URI must be an absolute HTTP(S) URI without a fragment");
        }
    }

    private List<String> effectiveScopes(List<String> requestedScopes) {
        Set<String> scopes = new HashSet<>();
        if (requestedScopes != null) {
            for (String scope : requestedScopes) {
                if (!ALLOWED_SCOPES.contains(scope)) {
                    throw new ManagementValidationException("Unsupported scope: " + scope);
                }
                scopes.add(scope);
            }
        }
        scopes.add(OidcScopes.OPENID);
        return scopes.stream().sorted().toList();
    }

    public record CreatedOAuthClient(
            String clientId,
            String clientName,
            Instant clientIdIssuedAt,
            List<String> redirectUris,
            List<String> scopes
    ) {
    }
}
