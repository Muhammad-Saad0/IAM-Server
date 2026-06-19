package com.example.iam.auth.adapter.security;

import com.example.iam.testsupport.IntegrationTestSupport;
import com.example.iam.testsupport.TestRsaKeys;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.test.web.servlet.MockMvc;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ManagementSecurityIntegrationTest extends IntegrationTestSupport {
    private static final String MANAGEMENT_PATH = "/api/management/accounts";

    @Autowired
    private MockMvc mockMvc;

    @Test
    void missingTokenReturnsStandardUnauthorizedError() throws Exception {
        mockMvc.perform(post(MANAGEMENT_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void validRsaTokenRequiresBothAdminRoleAndManagementScope() throws Exception {
        mockMvc.perform(post(MANAGEMENT_PATH)
                        .header(HttpHeaders.AUTHORIZATION, bearer(rsaToken(
                                ISSUER,
                                Instant.now().plusSeconds(300),
                                List.of("ADMIN"),
                                Set.of("iam.manage")
                        )))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));

        assertForbidden(rsaToken(
                ISSUER,
                Instant.now().plusSeconds(300),
                List.of("ADMIN"),
                Set.of("openid")
        ));
        assertForbidden(rsaToken(
                ISSUER,
                Instant.now().plusSeconds(300),
                List.of("USER"),
                Set.of("iam.manage")
        ));
        assertForbidden(rsaToken(
                ISSUER,
                Instant.now().plusSeconds(300),
                List.of("USER"),
                Set.of("openid")
        ));
    }

    @Test
    void nonRsaExpiredAndWrongIssuerTokensAreUnauthorized() throws Exception {
        assertUnauthorized(hsToken());
        assertUnauthorized(rsaToken(
                ISSUER,
                Instant.now().minusSeconds(120),
                List.of("ADMIN"),
                Set.of("iam.manage")
        ));
        assertUnauthorized(rsaToken(
                "https://wrong-issuer.example.test",
                Instant.now().plusSeconds(300),
                List.of("ADMIN"),
                Set.of("iam.manage")
        ));
    }

    @Test
    void corsAllowsOnlyConfiguredExactOrigins() throws Exception {
        mockMvc.perform(options(MANAGEMENT_PATH)
                        .header(HttpHeaders.ORIGIN, "http://localhost:3000")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, HttpMethod.POST.name()))
                .andExpect(status().isOk())
                .andExpect(header().string(
                        HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN,
                        "http://localhost:3000"
                ));

        mockMvc.perform(options(MANAGEMENT_PATH)
                        .header(HttpHeaders.ORIGIN, "https://evil.example.com")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, HttpMethod.POST.name()))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
    }

    private void assertForbidden(String token) throws Exception {
        mockMvc.perform(post(MANAGEMENT_PATH)
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"))
                .andExpect(jsonPath("$.status").value(403));
    }

    private void assertUnauthorized(String token) throws Exception {
        mockMvc.perform(post(MANAGEMENT_PATH)
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.status").value(401));
    }

    private String rsaToken(String issuer, Instant expiresAt, List<String> roles, Set<String> scopes) {
        RSAKey rsaKey = new RSAKey.Builder(TestRsaKeys.publicKey())
                .privateKey(TestRsaKeys.privateKey())
                .keyID("test-key")
                .build();
        JwtEncoder encoder = new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(rsaKey)));
        JwtClaimsSet claims = claims(issuer, expiresAt, roles, scopes);
        return encoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(SignatureAlgorithm.RS256).keyId("test-key").build(),
                claims
        )).getTokenValue();
    }

    private String hsToken() {
        SecretKeySpec key = new SecretKeySpec(
                "test-secret-test-secret-test-secret-test-secret".getBytes(StandardCharsets.UTF_8),
                "HmacSHA256"
        );
        JwtEncoder encoder = new NimbusJwtEncoder(new ImmutableSecret<>(key));
        return encoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).build(),
                claims(
                        ISSUER,
                        Instant.now().plusSeconds(300),
                        List.of("ADMIN"),
                        Set.of("iam.manage")
                )
        )).getTokenValue();
    }

    private JwtClaimsSet claims(
            String issuer,
            Instant expiresAt,
            List<String> roles,
            Set<String> scopes
    ) {
        Instant issuedAt = expiresAt.minusSeconds(300);
        return JwtClaimsSet.builder()
                .issuer(issuer)
                .subject(UUID.randomUUID().toString())
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .claim("roles", roles)
                .claim("scope", String.join(" ", scopes))
                .build();
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}
