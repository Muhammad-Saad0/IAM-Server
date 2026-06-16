package com.example.iam.auth.application.port.out;

public interface PasswordVerifier {
    boolean matches(String rawPassword, String passwordHash);
}
