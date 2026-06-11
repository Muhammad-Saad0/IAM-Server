package com.example.iam.auth.adapter.out.persistence;

import com.example.iam.auth.domain.model.AuthEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaAuthEventRepository extends JpaRepository<AuthEvent, Long> {
}
