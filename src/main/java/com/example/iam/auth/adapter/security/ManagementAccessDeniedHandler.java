package com.example.iam.auth.adapter.security;

import com.example.iam.auth.adapter.in.web.ApiErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Clock;

@Component
@RequiredArgsConstructor
public class ManagementAccessDeniedHandler implements AccessDeniedHandler {
    private final ObjectMapper objectMapper;
    private final Clock clock;

    @Override
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            AccessDeniedException accessDeniedException
    ) throws IOException, ServletException {
        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), new ApiErrorResponse(
                "ACCESS_DENIED",
                "Required role or scope is missing",
                HttpStatus.FORBIDDEN.value(),
                clock.instant()
        ));
    }
}
