package com.example.iam.management.adapter.in.web;

import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.example.iam.account.application.exception.AccountAlreadyExistsException;
import com.example.iam.account.application.service.AccountManagementService;
import com.example.iam.account.domain.model.AccountStatus;
import com.example.iam.auth.adapter.in.web.AuthExceptionHandler;
import com.example.iam.auth.application.exception.ManagementValidationException;
import com.example.iam.auth.application.service.OAuthClientManagementService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ManagementControllerTest {
    private static final Instant NOW = Instant.parse("2026-06-20T00:00:00Z");

    private AccountManagementService accountManagementService;
    private OAuthClientManagementService oAuthClientManagementService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        accountManagementService = mock(AccountManagementService.class);
        oAuthClientManagementService = mock(OAuthClientManagementService.class);
        ManagementController controller = new ManagementController(
                accountManagementService,
                oAuthClientManagementService
        );
        JsonMapper objectMapper = JsonMapper.builder()
                .addModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .build();
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new AuthExceptionHandler(Clock.fixed(NOW, ZoneOffset.UTC)))
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .build();
    }

    @Test
    void createsAccountAndReturnsCreatedRepresentation() throws Exception {
        UUID accountId = UUID.randomUUID();
        when(accountManagementService.createAccount(anyString(), anyString(), anyList()))
                .thenReturn(new AccountManagementService.CreatedAccount(
                        accountId,
                        "user@example.com",
                        AccountStatus.ACTIVE,
                        List.of("USER"),
                        NOW
                ));

        mockMvc.perform(post("/api/management/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "User@Example.COM",
                                  "password": "initial-password",
                                  "roles": ["USER"]
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(accountId.toString()))
                .andExpect(jsonPath("$.email").value("user@example.com"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.roles[0]").value("USER"))
                .andExpect(jsonPath("$.createdAt").value(NOW.toString()));
    }

    @Test
    void accountValidationFailuresUseStandardErrorShape() throws Exception {
        mockMvc.perform(post("/api/management/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "not-an-email",
                                  "password": "short",
                                  "roles": []
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.timestamp").value(NOW.toString()));
    }

    @Test
    void nullRoleAndMalformedJsonUseValidationErrorShape() throws Exception {
        mockMvc.perform(post("/api/management/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "user@example.com",
                                  "password": "initial-password",
                                  "roles": [null]
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));

        mockMvc.perform(post("/api/management/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    void duplicateAccountUsesConflictErrorCode() throws Exception {
        when(accountManagementService.createAccount(anyString(), anyString(), anyList()))
                .thenThrow(new AccountAlreadyExistsException());

        mockMvc.perform(post("/api/management/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "user@example.com",
                                  "password": "initial-password",
                                  "roles": ["USER"]
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ACCOUNT_ALREADY_EXISTS"))
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void createsOAuthClientAndReturnsEffectiveScopes() throws Exception {
        when(oAuthClientManagementService.createClient(anyString(), anyList(), anyList()))
                .thenReturn(new OAuthClientManagementService.CreatedOAuthClient(
                        UUID.randomUUID().toString(),
                        "Reporting UI",
                        NOW,
                        List.of("https://example.com/oauth/callback"),
                        List.of("email", "openid")
                ));

        mockMvc.perform(post("/api/management/oauth-clients")
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
                .andExpect(jsonPath("$.clientIdIssuedAt").value(NOW.toString()))
                .andExpect(jsonPath("$.redirectUris[0]").value("https://example.com/oauth/callback"))
                .andExpect(jsonPath("$.scopes[0]").value("email"))
                .andExpect(jsonPath("$.scopes[1]").value("openid"));
    }

    @Test
    void OAuthClientDomainValidationUsesValidationErrorCode() throws Exception {
        when(oAuthClientManagementService.createClient(anyString(), anyList(), anyList()))
                .thenThrow(new ManagementValidationException("Invalid redirect URI"));

        mockMvc.perform(post("/api/management/oauth-clients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "clientName": "Reporting UI",
                                  "redirectUris": ["/callback"],
                                  "scopes": []
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.status").value(400));
    }
}
