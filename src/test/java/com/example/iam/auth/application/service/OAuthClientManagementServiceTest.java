package com.example.iam.auth.application.service;

import com.example.iam.auth.application.exception.ManagementValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.oidc.OidcScopes;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OAuthClientManagementServiceTest {
    private static final Instant NOW = Instant.parse("2026-06-20T00:00:00Z");

    private CapturingRegisteredClientRepository repository;
    private OAuthClientManagementService service;

    @BeforeEach
    void setUp() {
        repository = new CapturingRegisteredClientRepository();
        service = new OAuthClientManagementService(
                repository,
                Clock.fixed(NOW, ZoneOffset.UTC)
        );
    }

    @Test
    void createsPublicAuthorizationCodeClientRequiringPkce() {
        OAuthClientManagementService.CreatedOAuthClient result = service.createClient(
                "Reporting UI",
                List.of("https://example.com/oauth/callback"),
                List.of(OidcScopes.EMAIL)
        );

        UUID.fromString(repository.saved.getId());
        UUID.fromString(repository.saved.getClientId());
        assertThat(repository.saved.getClientSecret()).isNull();
        assertThat(repository.saved.getClientAuthenticationMethods())
                .containsExactly(ClientAuthenticationMethod.NONE);
        assertThat(repository.saved.getAuthorizationGrantTypes())
                .containsExactly(AuthorizationGrantType.AUTHORIZATION_CODE);
        assertThat(repository.saved.getRedirectUris())
                .containsExactly("https://example.com/oauth/callback");
        assertThat(repository.saved.getScopes())
                .containsExactlyInAnyOrder(OidcScopes.OPENID, OidcScopes.EMAIL);
        assertThat(repository.saved.getClientSettings().isRequireProofKey()).isTrue();
        assertThat(repository.saved.getClientSettings().isRequireAuthorizationConsent()).isFalse();

        assertThat(result.clientId()).isEqualTo(repository.saved.getClientId());
        assertThat(result.clientName()).isEqualTo("Reporting UI");
        assertThat(result.clientIdIssuedAt()).isEqualTo(NOW);
        assertThat(result.redirectUris()).containsExactly("https://example.com/oauth/callback");
        assertThat(result.scopes()).containsExactly(OidcScopes.EMAIL, OidcScopes.OPENID);
    }

    @Test
    void openidIsAlwaysAdded() {
        OAuthClientManagementService.CreatedOAuthClient result = service.createClient(
                "Reporting UI",
                List.of("http://localhost:3000/callback"),
                List.of()
        );

        assertThat(result.scopes()).containsExactly(OidcScopes.OPENID);
    }

    @Test
    void rejectsRelativeFragmentedAndDuplicateRedirectUris() {
        assertThatThrownBy(() -> service.createClient(
                "Reporting UI",
                List.of("/callback"),
                List.of()
        )).isInstanceOf(ManagementValidationException.class);

        assertThatThrownBy(() -> service.createClient(
                "Reporting UI",
                List.of("https://example.com/callback#fragment"),
                List.of()
        )).isInstanceOf(ManagementValidationException.class);

        assertThatThrownBy(() -> service.createClient(
                "Reporting UI",
                List.of(
                        "https://example.com/callback",
                        "https://example.com/callback"
                ),
                List.of()
        )).isInstanceOf(ManagementValidationException.class);
    }

    @Test
    void rejectsForbiddenScopes() {
        assertThatThrownBy(() -> service.createClient(
                "Reporting UI",
                List.of("https://example.com/callback"),
                List.of("iam.manage")
        )).isInstanceOf(ManagementValidationException.class);

        assertThatThrownBy(() -> service.createClient(
                "Reporting UI",
                List.of("https://example.com/callback"),
                List.of(OidcScopes.PROFILE)
        )).isInstanceOf(ManagementValidationException.class);
    }

    private static final class CapturingRegisteredClientRepository implements RegisteredClientRepository {
        private RegisteredClient saved;

        @Override
        public void save(RegisteredClient registeredClient) {
            saved = registeredClient;
        }

        @Override
        public RegisteredClient findById(String id) {
            return null;
        }

        @Override
        public RegisteredClient findByClientId(String clientId) {
            return null;
        }
    }
}
