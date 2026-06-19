package com.example.iam.account.adapter.out.persistence;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class EmailNormalizationMigrationTest {
    @Test
    void migrationFailsOnConflictsThenNormalizesAndAddsCaseInsensitiveUniqueness() throws IOException {
        String migration = Files.readString(Path.of(
                "src/main/resources/db/migration/V6__normalize_user_emails.sql"
        ));

        assertThat(migration)
                .contains("group by lower(email)")
                .contains("raise exception")
                .contains("update users")
                .contains("set email = lower(email)")
                .contains("unique index")
                .contains("lower(email)");
    }
}
