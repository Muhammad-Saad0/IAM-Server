package com.example.iam.management.adapter.in.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CreateOAuthClientRequest(
        @NotBlank @Size(max = 200) String clientName,
        @NotEmpty List<@NotBlank String> redirectUris,
        List<@NotBlank String> scopes
) {
}
