package com.example.iam.auth.adapter.in.bootstrap;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.oidc.OidcScopes;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.UUID;

@Component
public class OAuth2ClientBootstrap implements ApplicationRunner {
    private static final String ADMIN_UI_CLIENT_ID = "admin-ui";

    private final RegisteredClientRepository registeredClientRepository;
    private final Clock clock;

    public OAuth2ClientBootstrap(RegisteredClientRepository registeredClientRepository, Clock clock) {
        this.registeredClientRepository = registeredClientRepository;
        this.clock = clock;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (registeredClientRepository.findByClientId(ADMIN_UI_CLIENT_ID) != null) {
            return;
        }

        RegisteredClient adminUiClient = RegisteredClient.withId(UUID.randomUUID().toString())
                .clientId(ADMIN_UI_CLIENT_ID)
                .clientIdIssuedAt(clock.instant())
                .clientName("Admin UI")
                .clientAuthenticationMethod(ClientAuthenticationMethod.NONE)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                // IAM will only send authorization codes for this client back to registered callback URLs.
                .redirectUri("http://localhost:3000/oauth/callback")
                .scope(OidcScopes.OPENID)
                .scope(OidcScopes.PROFILE)
                .scope(OidcScopes.EMAIL)
                .clientSettings(ClientSettings.builder()
                        .requireProofKey(true)
                        .requireAuthorizationConsent(false)
                        .build())
                .build();

        registeredClientRepository.save(adminUiClient);
    }
}
