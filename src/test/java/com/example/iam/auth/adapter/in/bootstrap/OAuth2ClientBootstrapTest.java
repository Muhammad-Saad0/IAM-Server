package com.example.iam.auth.adapter.in.bootstrap;

import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.oidc.OidcScopes;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class OAuth2ClientBootstrapTest {
    @Test
    void existingAdminUiGainsManagementScopeWithoutReplacingSettings() {
        Instant issuedAt = Instant.parse("2026-06-20T00:00:00Z");
        RegisteredClient existing = RegisteredClient.withId(UUID.randomUUID().toString())
                .clientId("admin-ui")
                .clientIdIssuedAt(issuedAt)
                .clientName("Existing Admin UI")
                .clientAuthenticationMethod(ClientAuthenticationMethod.NONE)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("https://admin.example.com/callback")
                .scope(OidcScopes.OPENID)
                .scope(OidcScopes.EMAIL)
                .clientSettings(ClientSettings.builder()
                        .requireProofKey(true)
                        .requireAuthorizationConsent(false)
                        .build())
                .build();
        CapturingRegisteredClientRepository repository = new CapturingRegisteredClientRepository(existing);
        OAuth2ClientBootstrap bootstrap = new OAuth2ClientBootstrap(
                repository,
                Clock.fixed(issuedAt.plusSeconds(60), ZoneOffset.UTC)
        );

        bootstrap.run(new DefaultApplicationArguments());

        assertThat(repository.saved).isNotNull();
        assertThat(repository.saved.getId()).isEqualTo(existing.getId());
        assertThat(repository.saved.getClientIdIssuedAt()).isEqualTo(issuedAt);
        assertThat(repository.saved.getClientName()).isEqualTo("Existing Admin UI");
        assertThat(repository.saved.getRedirectUris()).containsExactly("https://admin.example.com/callback");
        assertThat(repository.saved.getScopes()).containsExactlyInAnyOrder(
                OidcScopes.OPENID,
                OidcScopes.EMAIL,
                "iam.manage"
        );
        assertThat(repository.saved.getClientSettings().isRequireProofKey()).isTrue();
    }

    private static final class CapturingRegisteredClientRepository implements RegisteredClientRepository {
        private final RegisteredClient existing;
        private RegisteredClient saved;

        private CapturingRegisteredClientRepository(RegisteredClient existing) {
            this.existing = existing;
        }

        @Override
        public void save(RegisteredClient registeredClient) {
            saved = registeredClient;
        }

        @Override
        public RegisteredClient findById(String id) {
            return existing.getId().equals(id) ? existing : null;
        }

        @Override
        public RegisteredClient findByClientId(String clientId) {
            return existing.getClientId().equals(clientId) ? existing : null;
        }
    }
}
