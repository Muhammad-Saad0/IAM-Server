package com.example.iam.auth.adapter.security;

import com.example.iam.account.application.service.AccountManagementService;
import com.example.iam.testsupport.IntegrationTestSupport;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class OAuthPkceManagementFlowIntegrationTest extends IntegrationTestSupport {
    private static final String REDIRECT_URI = "http://localhost:3000/oauth/callback";
    private static final String CODE_VERIFIER =
            "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789-._~pkce";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AccountManagementService accountManagementService;

    @Autowired
    @Qualifier("authorizationServerJwtDecoder")
    private JwtDecoder jwtDecoder;

    @Test
    void authorizationCodePkceTokenCarriesClaimsAndAuthorizesManagementApi() throws Exception {
        String email = createAdmin();
        MockHttpSession session = loginThroughForm(email.toUpperCase(), "initial-password");
        String codeChallenge = codeChallenge(CODE_VERIFIER);

        MvcResult authorization = mockMvc.perform(get("/oauth2/authorize")
                        .session(session)
                        .queryParam("response_type", "code")
                        .queryParam("client_id", "admin-ui")
                        .queryParam("redirect_uri", REDIRECT_URI)
                        .queryParam("scope", "openid email iam.manage")
                        .queryParam("state", "test-state")
                        .queryParam("code_challenge", codeChallenge)
                        .queryParam("code_challenge_method", "S256"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern(REDIRECT_URI + "?code=*&state=test-state"))
                .andReturn();

        String authorizationCode = queryParameters(authorization.getResponse().getRedirectedUrl()).get("code");
        String tokenJson = mockMvc.perform(post("/oauth2/token")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("grant_type", "authorization_code")
                        .param("client_id", "admin-ui")
                        .param("redirect_uri", REDIRECT_URI)
                        .param("code", authorizationCode)
                        .param("code_verifier", CODE_VERIFIER))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode tokenResponse = objectMapper.readTree(tokenJson);
        String accessToken = tokenResponse.get("access_token").asText();
        String idToken = tokenResponse.get("id_token").asText();
        Jwt accessJwt = jwtDecoder.decode(accessToken);
        Jwt idJwt = jwtDecoder.decode(idToken);

        assertThat(accessJwt.getClaimAsStringList("roles")).contains("ADMIN");
        assertThat(accessJwt.getClaimAsString("scope")).contains("iam.manage");
        assertThat(idJwt.getClaimAsStringList("roles")).contains("ADMIN");
        assertThat(idJwt.getClaimAsString("email")).isEqualTo(email);

        mockMvc.perform(post("/api/management/accounts")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    void legacyHs256LoginRemainsCaseInsensitiveAndCompatibleWithAuthMe() throws Exception {
        String email = createUser();
        String loginJson = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("User-Agent", "integration-test")
                        .content("""
                                {
                                  "email": "%s",
                                  "password": "initial-password"
                                }
                                """.formatted(email.toUpperCase())))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String accessToken = objectMapper.readTree(loginJson).get("accessToken").asText();
        mockMvc.perform(get("/auth/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.roles[0]").value("USER"));
    }

    private String createAdmin() {
        String email = UUID.randomUUID() + "@example.com";
        accountManagementService.createAccount(
                email,
                "initial-password",
                List.of("ADMIN")
        );
        return email;
    }

    private String createUser() {
        String email = UUID.randomUUID() + "@example.com";
        accountManagementService.createAccount(
                email,
                "initial-password",
                List.of("USER")
        );
        return email;
    }

    private MockHttpSession loginThroughForm(String email, String password) throws Exception {
        MvcResult login = mockMvc.perform(post("/login")
                        .with(csrf())
                        .param("username", email)
                        .param("password", password))
                .andExpect(status().is3xxRedirection())
                .andReturn();
        HttpSession session = login.getRequest().getSession(false);
        assertThat(session).isInstanceOf(MockHttpSession.class);
        return (MockHttpSession) session;
    }

    private String codeChallenge(String verifier) throws Exception {
        byte[] digest = MessageDigest.getInstance("SHA-256")
                .digest(verifier.getBytes(StandardCharsets.US_ASCII));
        return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
    }

    private Map<String, String> queryParameters(String url) {
        return Arrays.stream(URI.create(url).getRawQuery().split("&"))
                .map(parameter -> parameter.split("=", 2))
                .collect(Collectors.toMap(
                        pair -> URLDecoder.decode(pair[0], StandardCharsets.UTF_8),
                        pair -> URLDecoder.decode(pair[1], StandardCharsets.UTF_8)
                ));
    }
}
