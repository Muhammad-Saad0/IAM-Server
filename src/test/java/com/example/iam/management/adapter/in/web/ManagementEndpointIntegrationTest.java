package com.example.iam.management.adapter.in.web;

import com.example.iam.account.adapter.out.persistence.JpaUserRepository;
import com.example.iam.account.adapter.out.persistence.JpaUserRoleRepository;
import com.example.iam.account.domain.model.User;
import com.example.iam.testsupport.IntegrationTestSupport;
import com.example.iam.testsupport.TestRsaKeys;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.oidc.OidcScopes;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ManagementEndpointIntegrationTest extends IntegrationTestSupport {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JpaUserRepository userRepository;

    @Autowired
    private JpaUserRoleRepository userRoleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private RegisteredClientRepository registeredClientRepository;

    @Test
    void accountCreationPersistsNormalizedAccountBcryptHashAndDistinctRoles() throws Exception {
        String localPart = UUID.randomUUID().toString();
        String requestedEmail = localPart.toUpperCase() + "@Example.COM";
        String normalizedEmail = requestedEmail.toLowerCase();

        mockMvc.perform(post("/api/management/accounts")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "%s",
                                  "password": "initial-password",
                                  "roles": ["USER", "USER", "ADMIN"]
                                }
                                """.formatted(requestedEmail)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value(normalizedEmail))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.roles[0]").value("ADMIN"))
                .andExpect(jsonPath("$.roles[1]").value("USER"));

        User user = userRepository.findByEmailIgnoreCase(requestedEmail).orElseThrow();
        assertThat(user.getPasswordHash()).doesNotContain("initial-password");
        assertThat(passwordEncoder.matches("initial-password", user.getPasswordHash())).isTrue();
        assertThat(userRoleRepository.findRoleNamesByUserId(user.getId()))
                .containsExactlyInAnyOrder("ADMIN", "USER");
    }

    @Test
    void duplicateAccountEmailIsRejectedCaseInsensitively() throws Exception {
        String localPart = UUID.randomUUID().toString();
        createAccount(localPart + "@example.com");

        mockMvc.perform(post("/api/management/accounts")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "%s@EXAMPLE.COM",
                                  "password": "initial-password",
                                  "roles": ["USER"]
                                }
                                """.formatted(localPart.toUpperCase())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ACCOUNT_ALREADY_EXISTS"));
    }

    @Test
    void oauthClientCreationPersistsPublicPkceClientAndEffectiveScopes() throws Exception {
        String responseJson = mockMvc.perform(post("/api/management/oauth-clients")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "clientName": "Reporting UI",
                                  "redirectUris": ["https://example.com/oauth/callback"],
                                  "scopes": ["email"]
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.clientName").value("Reporting UI"))
                .andExpect(jsonPath("$.scopes[0]").value("email"))
                .andExpect(jsonPath("$.scopes[1]").value("openid"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode response = objectMapper.readTree(responseJson);
        String clientId = response.get("clientId").asText();
        UUID.fromString(clientId);
        RegisteredClient client = registeredClientRepository.findByClientId(clientId);

        assertThat(client).isNotNull();
        UUID.fromString(client.getId());
        assertThat(client.getClientSecret()).isNull();
        assertThat(client.getClientAuthenticationMethods()).containsExactly(ClientAuthenticationMethod.NONE);
        assertThat(client.getAuthorizationGrantTypes()).containsExactly(AuthorizationGrantType.AUTHORIZATION_CODE);
        assertThat(client.getRedirectUris()).containsExactly("https://example.com/oauth/callback");
        assertThat(client.getScopes()).containsExactlyInAnyOrder(OidcScopes.OPENID, OidcScopes.EMAIL);
        assertThat(client.getScopes()).doesNotContain("iam.manage", OidcScopes.PROFILE);
        assertThat(client.getClientSettings().isRequireProofKey()).isTrue();
        assertThat(client.getClientSettings().isRequireAuthorizationConsent()).isFalse();
    }

    @Test
    void oauthClientEndpointRejectsForbiddenScopesAndInvalidRedirects() throws Exception {
        mockMvc.perform(post("/api/management/oauth-clients")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "clientName": "Reporting UI",
                                  "redirectUris": ["https://example.com/oauth/callback"],
                                  "scopes": ["iam.manage"]
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));

        mockMvc.perform(post("/api/management/oauth-clients")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "clientName": "Reporting UI",
                                  "redirectUris": ["https://example.com/oauth/callback#fragment"],
                                  "scopes": []
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    private void createAccount(String email) throws Exception {
        mockMvc.perform(post("/api/management/accounts")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "%s",
                                  "password": "initial-password",
                                  "roles": ["USER"]
                                }
                                """.formatted(email)))
                .andExpect(status().isCreated());
    }

    private String bearerToken() {
        RSAKey rsaKey = new RSAKey.Builder(TestRsaKeys.publicKey())
                .privateKey(TestRsaKeys.privateKey())
                .keyID("test-key")
                .build();
        NimbusJwtEncoder encoder = new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(rsaKey)));
        Instant issuedAt = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(ISSUER)
                .subject(UUID.randomUUID().toString())
                .issuedAt(issuedAt)
                .expiresAt(issuedAt.plusSeconds(300))
                .claim("roles", List.of("ADMIN"))
                .claim("scope", String.join(" ", Set.of("iam.manage")))
                .build();
        String token = encoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(SignatureAlgorithm.RS256).keyId("test-key").build(),
                claims
        )).getTokenValue();
        return "Bearer " + token;
    }
}
