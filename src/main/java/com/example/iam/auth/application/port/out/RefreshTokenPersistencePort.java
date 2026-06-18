package com.example.iam.auth.application.port.out;

import com.example.iam.auth.domain.model.RefreshToken;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenPersistencePort {
    RefreshToken save(RefreshToken refreshToken);

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    List<RefreshToken> findAllByTokenFamilyId(UUID tokenFamilyId);

    List<RefreshToken> findAllByTokenFamilyIdForUpdate(UUID tokenFamilyId);
}
