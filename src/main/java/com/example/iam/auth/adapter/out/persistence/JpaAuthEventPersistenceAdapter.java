package com.example.iam.auth.adapter.out.persistence;

import com.example.iam.auth.application.port.out.AuthEventPersistencePort;
import com.example.iam.auth.domain.model.AuthEvent;
import org.springframework.stereotype.Component;

@Component
public class JpaAuthEventPersistenceAdapter implements AuthEventPersistencePort {
    private final JpaAuthEventRepository repository;

    public JpaAuthEventPersistenceAdapter(JpaAuthEventRepository repository) {
        this.repository = repository;
    }

    @Override
    public AuthEvent save(AuthEvent authEvent) {
        return repository.save(authEvent);
    }
}
