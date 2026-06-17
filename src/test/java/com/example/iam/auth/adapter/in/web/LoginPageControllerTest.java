package com.example.iam.auth.adapter.in.web;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.DefaultCsrfToken;
import org.springframework.validation.support.BindingAwareModelMap;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class LoginPageControllerTest {
    private final LoginPageController controller = new LoginPageController();

    @Test
    void loginPageReturnsTemplateWithSpringSecurityModelAttributes() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute(CsrfToken.class.getName(), new DefaultCsrfToken("X-CSRF-TOKEN", "_csrf", "csrf-token"));
        BindingAwareModelMap model = new BindingAwareModelMap();

        String viewName = controller.login(null, null, request, model);

        assertThat(viewName).isEqualTo("login");
        assertThat(model)
                .containsEntry("loginError", false)
                .containsEntry("logoutSuccess", false)
                .containsEntry("csrfParameterName", "_csrf")
                .containsEntry("csrfToken", "csrf-token");
    }

    @Test
    void loginPageShowsErrorState() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        BindingAwareModelMap model = new BindingAwareModelMap();

        String viewName = controller.login("", null, request, model);

        assertThat(viewName).isEqualTo("login");
        assertThat(model)
                .containsEntry("loginError", true)
                .containsEntry("logoutSuccess", false);
    }

    @Test
    void loginPageShowsLogoutState() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        BindingAwareModelMap model = new BindingAwareModelMap();

        String viewName = controller.login(null, "", request, model);

        assertThat(viewName).isEqualTo("login");
        assertThat(model)
                .containsEntry("loginError", false)
                .containsEntry("logoutSuccess", true);
    }

    @Test
    void thymeleafTemplateKeepsSpringSecurityFormContract() throws IOException {
        String template = Files.readString(Path.of("src/main/resources/templates/login.html"));

        assertThat(template)
                .contains("method=\"post\"")
                .contains("th:action=\"@{/login}\"")
                .contains("name=\"username\"")
                .contains("name=\"password\"")
                .contains("th:name=\"${csrfParameterName}\"")
                .contains("th:value=\"${csrfToken}\"")
                .contains("@{/login-assets/css/main.css}");
    }
}
