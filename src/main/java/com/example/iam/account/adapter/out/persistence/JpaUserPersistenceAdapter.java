package com.example.iam.account.adapter.out.persistence;

import com.example.iam.account.application.port.out.UserPersistencePort;
import com.example.iam.account.domain.model.User;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class JpaUserPersistenceAdapter implements UserPersistencePort {
    private final JpaUserRepository repository;

    public JpaUserPersistenceAdapter(JpaUserRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return repository.findByEmail(email);
    }
}
