package com.example.iam.management.adapter.in.web;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CreateAccountRequest(
        @Email @NotBlank @Size(max = 320) String email,
        @NotBlank @Size(min = 8, max = 128) String password,
        @NotEmpty List<@NotNull @Pattern(regexp = "USER|ADMIN") String> roles
) {
}
