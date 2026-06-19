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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AccountManagementServiceTest {
    private static final Instant NOW = Instant.parse("2026-06-20T00:00:00Z");

    private FakeUserPersistencePort userPersistencePort;
    private FakeUserRolePersistencePort userRolePersistencePort;
    private AccountManagementService service;

    @BeforeEach
    void setUp() {
        userPersistencePort = new FakeUserPersistencePort();
        userRolePersistencePort = new FakeUserRolePersistencePort();
        service = new AccountManagementService(
                userPersistencePort,
                rolePersistencePort(),
                userRolePersistencePort,
                rawPassword -> "bcrypt:" + rawPassword,
                Clock.fixed(NOW, ZoneOffset.UTC)
        );
    }

    @Test
    void createsActiveAccountWithNormalizedEmailHashAndDistinctRoles() {
        AccountManagementService.CreatedAccount result = service.createAccount(
                " New.User@Example.COM ",
                "initial-password",
                List.of("USER", "USER", "ADMIN")
        );

        assertThat(result.email()).isEqualTo("new.user@example.com");
        assertThat(result.status()).isEqualTo(AccountStatus.ACTIVE);
        assertThat(result.roles()).containsExactly("ADMIN", "USER");
        assertThat(result.createdAt()).isEqualTo(NOW);
        assertThat(userPersistencePort.saved.getPasswordHash()).isEqualTo("bcrypt:initial-password");
        assertThat(userRolePersistencePort.saved)
                .extracting(userRole -> userRole.getRole().getName())
                .containsExactlyInAnyOrder("ADMIN", "USER");
    }

    @Test
    void duplicateEmailIsRejectedCaseInsensitively() {
        userPersistencePort.existing = User.register(
                "user@example.com",
                "existing-hash",
                NOW.minusSeconds(60)
        );

        assertThatThrownBy(() -> service.createAccount(
                "USER@EXAMPLE.COM",
                "initial-password",
                List.of("USER")
        )).isInstanceOf(AccountAlreadyExistsException.class);

        assertThat(userPersistencePort.saved).isNull();
    }

    @Test
    void databaseDuplicateRaceIsTranslatedToAccountAlreadyExists() {
        userPersistencePort.failOnSave = true;

        assertThatThrownBy(() -> service.createAccount(
                "user@example.com",
                "initial-password",
                List.of("USER")
        )).isInstanceOf(AccountAlreadyExistsException.class);
    }

    private RolePersistencePort rolePersistencePort() {
        Map<String, Role> roles = new HashMap<>();
        roles.put("ADMIN", role(1L, "ADMIN"));
        roles.put("USER", role(2L, "USER"));
        return name -> Optional.ofNullable(roles.get(name));
    }

    private Role role(long id, String name) {
        Role role = mock(Role.class);
        when(role.getId()).thenReturn(id);
        when(role.getName()).thenReturn(name);
        return role;
    }

    private static final class FakeUserPersistencePort implements UserPersistencePort {
        private User existing;
        private User saved;
        private boolean failOnSave;

        @Override
        public Optional<User> findByEmail(String email) {
            return Optional.ofNullable(existing);
        }

        @Override
        public User save(User user) {
            if (failOnSave) {
                throw new DataIntegrityViolationException("duplicate email");
            }
            saved = user;
            return user;
        }
    }

    private static final class FakeUserRolePersistencePort implements UserRolePersistencePort {
        private final List<UserRole> saved = new ArrayList<>();

        @Override
        public List<String> findRoleNamesByUserId(java.util.UUID userId) {
            return List.of();
        }

        @Override
        public UserRole save(UserRole userRole) {
            saved.add(userRole);
            return userRole;
        }
    }
}
