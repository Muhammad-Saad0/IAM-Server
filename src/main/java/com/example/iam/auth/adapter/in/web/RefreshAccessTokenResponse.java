package com.example.iam.auth.adapter.in.web;

import java.time.Instant;

public record RefreshAccessTokenResponse(
        String accessToken,
        Instant accessTokenExpiresAt
) {
}
