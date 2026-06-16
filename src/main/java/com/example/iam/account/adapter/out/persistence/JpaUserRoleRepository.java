package com.example.iam.account.adapter.out.persistence;

import com.example.iam.account.domain.model.UserRole;
import com.example.iam.account.domain.model.UserRoleId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface JpaUserRoleRepository extends JpaRepository<UserRole, UserRoleId> {
    @Query("""
            select userRole.role.name
            from UserRole userRole
            where userRole.user.id = :userId
            """)
    List<String> findRoleNamesByUserId(@Param("userId") UUID userId);
}
