package com.example.iam.auth.adapter.security;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.InsufficientAuthenticationException;

import static org.assertj.core.api.Assertions.assertThat;

class ApiAuthenticationEntryPointTest {
    private final ApiAuthenticationEntryPoint entryPoint = new ApiAuthenticationEntryPoint();

    @Test
    void unauthenticatedApiRequestReturnsBearerUnauthorizedResponse() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        entryPoint.commence(
                request,
                response,
                new InsufficientAuthenticationException("Authentication is required")
        );

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getHeader("WWW-Authenticate")).isEqualTo("Bearer");
    }
}
