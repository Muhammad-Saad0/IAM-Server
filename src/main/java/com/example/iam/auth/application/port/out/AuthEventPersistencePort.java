package com.example.iam.auth.application.port.out;

import com.example.iam.auth.domain.model.AuthEvent;

public interface AuthEventPersistencePort {
    AuthEvent save(AuthEvent authEvent);
}
