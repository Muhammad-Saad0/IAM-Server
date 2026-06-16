package com.example.iam.account.adapter.in.bootstrap;

import com.example.iam.account.application.port.out.RolePersistencePort;
import com.example.iam.account.application.port.out.UserPersistencePort;
import com.example.iam.account.application.port.out.UserRolePersistencePort;
import com.example.iam.account.domain.model.Role;
import com.example.iam.account.domain.model.User;
import com.example.iam.account.domain.model.UserRole;
import com.example.iam.auth.application.port.out.PasswordHasher;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.time.Clock;
import java.time.Instant;

@Component
public class AdminUserBootstrap implements ApplicationRunner {
    private static final String ADMIN_ROLE = "ADMIN";

    private final boolean enabled;
    private final String adminEmail;
    private final String adminPassword;
    private final UserPersistencePort userPersistencePort;
    private final RolePersistencePort rolePersistencePort;
    private final UserRolePersistencePort userRolePersistencePort;
    private final PasswordHasher passwordHasher;
    private final Clock clock;

    public AdminUserBootstrap(
            @Value("${app.bootstrap.admin.enabled:false}") boolean enabled,
            @Value("${app.bootstrap.admin.email:}") String adminEmail,
            @Value("${app.bootstrap.admin.password:}") String adminPassword,
            UserPersistencePort userPersistencePort,
            RolePersistencePort rolePersistencePort,
            UserRolePersistencePort userRolePersistencePort,
            PasswordHasher passwordHasher,
            Clock clock
    ) {
        this.enabled = enabled;
        this.adminEmail = adminEmail;
        this.adminPassword = adminPassword;
        this.userPersistencePort = userPersistencePort;
        this.rolePersistencePort = rolePersistencePort;
        this.userRolePersistencePort = userRolePersistencePort;
        this.passwordHasher = passwordHasher;
        this.clock = clock;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!enabled || adminEmail.isBlank() || adminPassword.isBlank()) {
            return;
        }

        if (userPersistencePort.findByEmail(adminEmail).isPresent()) {
            return;
        }

        Instant now = clock.instant();
        Role adminRole = rolePersistencePort.findByName(ADMIN_ROLE)
                .orElseThrow(() -> new IllegalStateException("Required role is missing: " + ADMIN_ROLE));

        User adminUser = userPersistencePort.save(User.register(adminEmail, passwordHasher.hash(adminPassword), now));
        userRolePersistencePort.save(new UserRole(adminUser, adminRole, now));
    }
}
