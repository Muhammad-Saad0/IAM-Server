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
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;

@Configuration
public class OAuth2JwkConfig {
    /*
     * Supplies Spring Authorization Server with the RSA key pair used to sign
     * OAuth/OIDC tokens. The private key stays in configuration, while the public
     * key is exposed later through the standard JWKS endpoint for token validation.
     */
    @Bean("authorizationServerRsaKey")
    RSAKey authorizationServerRsaKey(
            @Value("${app.oauth2.jwk.private-key}") String privateKeyPem,
            @Value("${app.oauth2.jwk.private-key-path}") String privateKeyPath,
            @Value("${app.oauth2.jwk.public-key}") String publicKeyPem,
            @Value("${app.oauth2.jwk.public-key-path}") String publicKeyPath,
            @Value("${app.oauth2.jwk.key-id}") String keyId
    ) {
        RSAPrivateKey privateKey = parsePrivateKey(requiredPem(
                keyMaterial(privateKeyPem, privateKeyPath),
                "app.oauth2.jwk.private-key or app.oauth2.jwk.private-key-path must be configured with a PKCS#8 PEM private key"
        ));
        RSAPublicKey publicKey = parsePublicKey(requiredPem(
                keyMaterial(publicKeyPem, publicKeyPath),
                "app.oauth2.jwk.public-key or app.oauth2.jwk.public-key-path must be configured with an X.509 PEM public key"
        ));

        RSAKey rsaKey = new RSAKey.Builder(publicKey)
                .privateKey(privateKey)
                .keyID(keyId)
                .build();

        return rsaKey;
    }

    @Bean
    JWKSource<SecurityContext> jwkSource(RSAKey authorizationServerRsaKey) {
        return new ImmutableJWKSet<>(new JWKSet(authorizationServerRsaKey));
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

    private String keyMaterial(String pem, String path) {
        if (pem != null && !pem.isBlank()) {
            return pem;
        }

        if (path == null || path.isBlank()) {
            return "";
        }

        try {
            return Files.readString(Path.of(path), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to read RSA key from " + path, exception);
        }
    }

    private RSAPrivateKey parsePrivateKey(String pem) {
        return RsaKeyConverters.pkcs8().convert(new ByteArrayInputStream(pem.getBytes(StandardCharsets.UTF_8)));
    }

    private RSAPublicKey parsePublicKey(String pem) {
        return RsaKeyConverters.x509().convert(new ByteArrayInputStream(pem.getBytes(StandardCharsets.UTF_8)));
    }
}
