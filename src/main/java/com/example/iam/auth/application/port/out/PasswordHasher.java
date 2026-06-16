package com.example.iam.auth.application.port.out;

public interface PasswordHasher {
    String hash(String rawPassword);
}
