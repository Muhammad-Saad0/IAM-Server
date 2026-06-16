package com.example.iam.account.application.port.out;

import com.example.iam.account.domain.model.User;

import java.util.Optional;

public interface UserPersistencePort {
    Optional<User> findByEmail(String email);

    User save(User user);
}
