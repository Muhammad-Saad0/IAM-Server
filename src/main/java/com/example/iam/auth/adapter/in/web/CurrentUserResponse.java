package com.example.iam.auth.adapter.in.web;

import java.util.List;

public record CurrentUserResponse(
        String userId,
        String email,
        List<String> roles
) {
}
