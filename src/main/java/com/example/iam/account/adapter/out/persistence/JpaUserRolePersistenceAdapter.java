package com.example.iam.account.adapter.out.persistence;

import com.example.iam.account.application.port.out.UserRolePersistencePort;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
public class JpaUserRolePersistenceAdapter implements UserRolePersistencePort {
    private final JpaUserRoleRepository repository;

    public JpaUserRolePersistenceAdapter(JpaUserRoleRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<String> findRoleNamesByUserId(UUID userId) {
        return repository.findRoleNamesByUserId(userId);
    }
}
