package com.example.iam.auth.adapter.out.persistence;

import com.example.iam.auth.application.port.out.RefreshTokenPersistencePort;
import com.example.iam.auth.domain.model.RefreshToken;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class JpaRefreshTokenPersistenceAdapter implements RefreshTokenPersistencePort {
    private final JpaRefreshTokenRepository repository;

    @Override
    public RefreshToken save(RefreshToken refreshToken) {
        return repository.save(refreshToken);
    }

    @Override
    public Optional<RefreshToken> findByTokenHash(String tokenHash) {
        return repository.findByTokenHash(tokenHash);
    }

    @Override
    public List<RefreshToken> findAllByTokenFamilyId(UUID tokenFamilyId) {
        return repository.findAllByTokenFamilyId(tokenFamilyId);
    }

    @Override
    public List<RefreshToken> findAllByTokenFamilyIdForUpdate(UUID tokenFamilyId) {
        return repository.findAllByTokenFamilyIdOrderByIdAsc(tokenFamilyId);
    }
}
