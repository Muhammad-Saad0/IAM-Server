package com.example.iam.auth.adapter.in.web;

import com.example.iam.auth.application.exception.InvalidCredentialsException;
import com.example.iam.auth.application.exception.InvalidRefreshTokenException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Clock;

@RestControllerAdvice
@RequiredArgsConstructor
public class AuthExceptionHandler {
    private static final String INVALID_CREDENTIALS_CODE = "INVALID_CREDENTIALS";
    private static final String INVALID_REFRESH_TOKEN_CODE = "INVALID_REFRESH_TOKEN";
    private static final String VALIDATION_FAILED_CODE = "VALIDATION_FAILED";

    private final Clock clock;

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidCredentials() {
        return error(
                HttpStatus.UNAUTHORIZED,
                INVALID_CREDENTIALS_CODE,
                "Invalid email or password"
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidationFailure() {
        return error(
                HttpStatus.BAD_REQUEST,
                VALIDATION_FAILED_CODE,
                "Request validation failed"
        );
    }

    @ExceptionHandler(InvalidRefreshTokenException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidRefreshToken() {
        return error(
                HttpStatus.UNAUTHORIZED,
                INVALID_REFRESH_TOKEN_CODE,
                "Invalid refresh token"
        );
    }

    private ResponseEntity<ApiErrorResponse> error(HttpStatus status, String code, String message) {
        return ResponseEntity.status(status)
                .body(new ApiErrorResponse(
                        code,
                        message,
                        status.value(),
                        clock.instant()
                ));
    }
}
