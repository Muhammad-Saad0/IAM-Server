package com.example.iam.account.application.port.out;

import com.example.iam.account.domain.model.UserRole;

import java.util.List;
import java.util.UUID;

public interface UserRolePersistencePort {
    List<String> findRoleNamesByUserId(UUID userId);

    UserRole save(UserRole userRole);
}
