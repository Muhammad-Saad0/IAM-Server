package com.example.iam.account.adapter.out.persistence;

import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class JpaUserPersistenceAdapterTest {
    @Test
    void emailLookupIsCaseInsensitive() {
        JpaUserRepository repository = mock(JpaUserRepository.class);
        JpaUserPersistenceAdapter adapter = new JpaUserPersistenceAdapter(repository);

        adapter.findByEmail("User@Example.COM");

        verify(repository).findByEmailIgnoreCase("User@Example.COM");
    }
}
