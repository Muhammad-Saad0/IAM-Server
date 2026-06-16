package com.example.iam.auth.adapter.in.web;

import java.time.Instant;

public record LoginResponse(
        String accessToken,
        Instant accessTokenExpiresAt
) {
}
