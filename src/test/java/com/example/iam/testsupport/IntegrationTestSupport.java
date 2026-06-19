package com.example.iam.testsupport;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

public abstract class IntegrationTestSupport {
    public static final String ISSUER = "https://issuer.example.test";

    @DynamicPropertySource
    static void oauthProperties(DynamicPropertyRegistry registry) {
        registry.add("app.oauth2.issuer", () -> ISSUER);
        registry.add("app.oauth2.jwk.private-key", TestRsaKeys::privateKeyPem);
        registry.add("app.oauth2.jwk.public-key", TestRsaKeys::publicKeyPem);
        registry.add("app.bootstrap.admin.enabled", () -> "false");
    }
}
