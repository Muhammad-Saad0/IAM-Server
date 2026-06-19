package com.example.iam.account.adapter.in.bootstrap;

import com.example.iam.account.application.port.out.RolePersistencePort;
import com.example.iam.account.application.port.out.UserPersistencePort;
import com.example.iam.account.application.port.out.UserRolePersistencePort;
import com.example.iam.account.domain.model.Role;
import com.example.iam.account.domain.model.User;
import com.example.iam.account.domain.model.UserRole;
import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultApplicationArguments;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AdminUserBootstrapTest {
    @Test
    void bootstrappedAdminEmailIsNormalized() {
        Instant now = Instant.parse("2026-06-20T00:00:00Z");
        CapturingUserPersistencePort users = new CapturingUserPersistencePort();
        Role adminRole = mock(Role.class);
        when(adminRole.getId()).thenReturn(1L);
        RolePersistencePort roles = name -> Optional.of(adminRole);
        UserRolePersistencePort userRoles = new UserRolePersistencePort() {
            @Override
            public List<String> findRoleNamesByUserId(java.util.UUID userId) {
                return List.of();
            }

            @Override
            public UserRole save(UserRole userRole) {
                return userRole;
            }
        };
        AdminUserBootstrap bootstrap = new AdminUserBootstrap(
                true,
                " Admin@Example.COM ",
                "admin-password",
                users,
                roles,
                userRoles,
                password -> "hash",
                Clock.fixed(now, ZoneOffset.UTC)
        );

        bootstrap.run(new DefaultApplicationArguments());

        assertThat(users.lookupEmail).isEqualTo("admin@example.com");
        assertThat(users.saved.getEmail()).isEqualTo("admin@example.com");
    }

    private static final class CapturingUserPersistencePort implements UserPersistencePort {
        private String lookupEmail;
        private User saved;

        @Override
        public Optional<User> findByEmail(String email) {
            lookupEmail = email;
            return Optional.empty();
        }

        @Override
        public User save(User user) {
            saved = user;
            return user;
        }
    }
}
