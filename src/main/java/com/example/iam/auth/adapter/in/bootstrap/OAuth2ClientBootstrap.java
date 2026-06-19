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
    private static final String MANAGEMENT_SCOPE = "iam.manage";
    private static final String LOCAL_REDIRECT_URI = "http://localhost:3000/oauth/callback";
    private static final String PRODUCTION_REDIRECT_URI =
            "https://iam-server-fe.vercel.app/oauth/callback";
    private static final String LOCAL_POST_LOGOUT_REDIRECT_URI = "http://localhost:3000/";
    private static final String PRODUCTION_POST_LOGOUT_REDIRECT_URI =
            "https://iam-server-fe.vercel.app/";

    private final RegisteredClientRepository registeredClientRepository;
    private final Clock clock;

    public OAuth2ClientBootstrap(RegisteredClientRepository registeredClientRepository, Clock clock) {
        this.registeredClientRepository = registeredClientRepository;
        this.clock = clock;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        RegisteredClient existingClient = registeredClientRepository.findByClientId(ADMIN_UI_CLIENT_ID);
        if (existingClient != null) {
            augmentAdminUiClient(existingClient);
            return;
        }

        RegisteredClient adminUiClient = RegisteredClient.withId(UUID.randomUUID().toString())
                .clientId(ADMIN_UI_CLIENT_ID)
                .clientIdIssuedAt(clock.instant())
                .clientName("Admin UI")
                .clientAuthenticationMethod(ClientAuthenticationMethod.NONE)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                // IAM will only send authorization codes for this client back to registered callback URLs.
                .redirectUri(LOCAL_REDIRECT_URI)
                .redirectUri(PRODUCTION_REDIRECT_URI)
                .postLogoutRedirectUri(LOCAL_POST_LOGOUT_REDIRECT_URI)
                .postLogoutRedirectUri(PRODUCTION_POST_LOGOUT_REDIRECT_URI)
                .scope(OidcScopes.OPENID)
                .scope(OidcScopes.PROFILE)
                .scope(OidcScopes.EMAIL)
                .scope(MANAGEMENT_SCOPE)
                .clientSettings(ClientSettings.builder()
                        .requireProofKey(true)
                        .requireAuthorizationConsent(false)
                        .build())
                .build();

        registeredClientRepository.save(adminUiClient);
    }

    private void augmentAdminUiClient(RegisteredClient existingClient) {
        RegisteredClient augmentedClient = RegisteredClient.from(existingClient)
                .redirectUri(LOCAL_REDIRECT_URI)
                .redirectUri(PRODUCTION_REDIRECT_URI)
                .postLogoutRedirectUri(LOCAL_POST_LOGOUT_REDIRECT_URI)
                .postLogoutRedirectUri(PRODUCTION_POST_LOGOUT_REDIRECT_URI)
                .scope(MANAGEMENT_SCOPE)
                .build();

        if (!augmentedClient.equals(existingClient)) {
            registeredClientRepository.save(augmentedClient);
        }
    }
}
