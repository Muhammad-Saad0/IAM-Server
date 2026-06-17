package com.example.iam.auth.adapter.in.web;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class DocsControllerTest {
    private final DocsController controller = new DocsController();

    @Test
    void docsReturnsDocsTemplate() {
        assertThat(controller.docs()).isEqualTo("docs");
    }

    @Test
    void docsTemplateDocumentsOauthAndApiContracts() throws IOException {
        String template = Files.readString(Path.of("src/main/resources/templates/docs.html"));

        assertThat(template)
                .contains("@{/docs-assets/css/docs.css}")
                .contains("GET /oauth2/authorize")
                .contains("POST /oauth2/token")
                .contains("GET /oauth2/jwks")
                .contains("GET /.well-known/openid-configuration")
                .contains("client_id=admin-ui")
                .contains("code_challenge_method=S256")
                .contains("POST /auth/login")
                .contains("POST /auth/refresh")
                .contains("GET /auth/me");
    }
}
