package com.example.iam.account.application.service;

import com.example.iam.account.application.exception.AccountAlreadyExistsException;
import com.example.iam.account.application.port.out.RolePersistencePort;
import com.example.iam.account.application.port.out.UserPersistencePort;
import com.example.iam.account.application.port.out.UserRolePersistencePort;
import com.example.iam.account.domain.model.AccountStatus;
import com.example.iam.account.domain.model.Role;
import com.example.iam.account.domain.model.User;
import com.example.iam.account.domain.model.UserRole;
import com.example.iam.auth.application.port.out.PasswordHasher;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AccountManagementService {
    private final UserPersistencePort userPersistencePort;
    private final RolePersistencePort rolePersistencePort;
    private final UserRolePersistencePort userRolePersistencePort;
    private final PasswordHasher passwordHasher;
    private final Clock clock;

    @Transactional
    public CreatedAccount createAccount(String email, String password, List<String> requestedRoles) {
        String normalizedEmail = normalizeEmail(email);
        if (userPersistencePort.findByEmail(normalizedEmail).isPresent()) {
            throw new AccountAlreadyExistsException();
        }

        List<String> roleNames = requestedRoles.stream()
                .distinct()
                .sorted()
                .toList();
        List<Role> roles = roleNames.stream()
                .map(this::requiredRole)
                .toList();
        Instant now = clock.instant();
        User user;
        try {
            user = userPersistencePort.save(User.register(
                    normalizedEmail,
                    passwordHasher.hash(password),
                    now
            ));
        } catch (DataIntegrityViolationException exception) {
            throw new AccountAlreadyExistsException(exception);
        }

        roles.forEach(role -> userRolePersistencePort.save(new UserRole(user, role, now)));

        return new CreatedAccount(
                user.getId(),
                user.getEmail(),
                user.getStatus(),
                roleNames,
                user.getCreatedAt()
        );
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private Role requiredRole(String roleName) {
        return rolePersistencePort.findByName(roleName)
                .orElseThrow(() -> new IllegalStateException("Required role is missing: " + roleName));
    }

    public record CreatedAccount(
            UUID id,
            String email,
            AccountStatus status,
            List<String> roles,
            Instant createdAt
    ) {
    }
}
