package com.example.iam.account.adapter.out.persistence;

import com.example.iam.account.application.port.out.RolePersistencePort;
import com.example.iam.account.domain.model.Role;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class JpaRolePersistenceAdapter implements RolePersistencePort {
    private final JpaRoleRepository repository;

    public JpaRolePersistenceAdapter(JpaRoleRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<Role> findByName(String name) {
        return repository.findByName(name);
    }
}
