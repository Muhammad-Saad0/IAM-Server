package com.example.iam.account.application.port.out;

import java.util.List;
import java.util.UUID;

public interface UserRolePersistencePort {
    List<String> findRoleNamesByUserId(UUID userId);
}
