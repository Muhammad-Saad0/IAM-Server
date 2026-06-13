package com.example.iam.auth.adapter.in.web;

import java.time.Instant;

public record ApiErrorResponse(
        String code,
        String message,
        int status,
        Instant timestamp
) {
}
