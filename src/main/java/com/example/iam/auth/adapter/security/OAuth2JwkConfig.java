package com.example.iam.auth.adapter.security;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.security.converter.RsaKeyConverters;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configuration.OAuth2AuthorizationServerConfiguration;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.UUID;

@Configuration
public class OAuth2JwkConfig {
    /*
     * Supplies Spring Authorization Server with the RSA key pair used to sign
     * OAuth/OIDC tokens. The private key stays in configuration, while the public
     * key is exposed later through the standard JWKS endpoint for token validation.
     */
    @Bean
    JWKSource<SecurityContext> jwkSource(
            @Value("${app.oauth2.jwk.private-key}") String privateKeyPem,
            @Value("${app.oauth2.jwk.public-key}") String publicKeyPem
    ) {
        RSAPrivateKey privateKey = parsePrivateKey(requiredPem(
                privateKeyPem,
                "app.oauth2.jwk.private-key must be configured with a PKCS#8 PEM private key"
        ));
        RSAPublicKey publicKey = parsePublicKey(requiredPem(
                publicKeyPem,
                "app.oauth2.jwk.public-key must be configured with an X.509 PEM public key"
        ));

        RSAKey rsaKey = new RSAKey.Builder(publicKey)
                .privateKey(privateKey)
                .keyID(UUID.randomUUID().toString())
                .build();

        return new ImmutableJWKSet<>(new JWKSet(rsaKey));
    }

    @Bean("authorizationServerJwtDecoder")
    @Primary
    JwtDecoder authorizationServerJwtDecoder(JWKSource<SecurityContext> jwkSource) {
        return OAuth2AuthorizationServerConfiguration.jwtDecoder(jwkSource);
    }

    @Bean("authorizationServerJwtEncoder")
    JwtEncoder authorizationServerJwtEncoder(JWKSource<SecurityContext> jwkSource) {
        return new NimbusJwtEncoder(jwkSource);
    }

    private String requiredPem(String pem, String message) {
        if (pem == null || pem.isBlank()) {
            throw new IllegalStateException(message);
        }

        return pem;
    }

    private RSAPrivateKey parsePrivateKey(String pem) {
        return RsaKeyConverters.pkcs8().convert(new ByteArrayInputStream(pem.getBytes(StandardCharsets.UTF_8)));
    }

    private RSAPublicKey parsePublicKey(String pem) {
        return RsaKeyConverters.x509().convert(new ByteArrayInputStream(pem.getBytes(StandardCharsets.UTF_8)));
    }
}
