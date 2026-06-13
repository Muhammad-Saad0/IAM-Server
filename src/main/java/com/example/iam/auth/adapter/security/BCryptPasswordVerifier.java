package com.example.iam.auth.adapter.security;

import com.example.iam.auth.application.port.out.PasswordHasher;
import com.example.iam.auth.application.port.out.PasswordVerifier;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class BCryptPasswordVerifier implements PasswordVerifier, PasswordHasher {
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Override
    public String hash(String rawPassword) {
        return passwordEncoder.encode(rawPassword);
    }

    @Override
    public boolean matches(String rawPassword, String passwordHash) {
        return passwordEncoder.matches(rawPassword, passwordHash);
    }
}
