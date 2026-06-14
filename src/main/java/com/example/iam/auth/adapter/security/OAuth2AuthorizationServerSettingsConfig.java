package com.example.iam.auth.adapter.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;

@Configuration
public class OAuth2AuthorizationServerSettingsConfig {
    @Bean
    AuthorizationServerSettings authorizationServerSettings(
            @Value("${app.oauth2.issuer:http://localhost:8080}") String issuer
    ) {
        return AuthorizationServerSettings.builder()
                .issuer(issuer)
                .build();
    }
}
