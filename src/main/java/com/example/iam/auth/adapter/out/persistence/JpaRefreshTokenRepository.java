package com.example.iam.auth.adapter.out.persistence;

import com.example.iam.auth.domain.model.RefreshToken;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface JpaRefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    Optional<RefreshToken> findByTokenHash(String tokenHash);

    List<RefreshToken> findAllByTokenFamilyId(UUID tokenFamilyId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<RefreshToken> findAllByTokenFamilyIdOrderByIdAsc(UUID tokenFamilyId);
}
