package com.example.iam.account.application.port.out;

import com.example.iam.account.domain.model.Role;

import java.util.Optional;

public interface RolePersistencePort {
    Optional<Role> findByName(String name);
}
