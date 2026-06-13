package com.example.iam.account.adapter.out.persistence;

import com.example.iam.account.domain.model.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface JpaRoleRepository extends JpaRepository<Role, Long> {
    Optional<Role> findByName(String name);
}
