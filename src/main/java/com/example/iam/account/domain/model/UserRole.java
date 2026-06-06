package com.example.iam.account.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "user_roles")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserRole {
    /*
     * user_roles is modeled as its own entity instead of @ManyToMany because
     * the assignment has data of its own: assignedAt.
     */
    @EmbeddedId
    private UserRoleId id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("userId")
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("roleId")
    @JoinColumn(name = "role_id", nullable = false)
    private Role role;

    @Column(name = "assigned_at", nullable = false)
    private Instant assignedAt;

    public UserRole(User user, Role role, Instant assignedAt) {
        this.user = Objects.requireNonNull(user, "user must not be null");
        this.role = Objects.requireNonNull(role, "role must not be null");
        this.assignedAt = Objects.requireNonNull(assignedAt, "assignedAt must not be null");
        this.id = new UserRoleId(
                Objects.requireNonNull(user.getId(), "user id must not be null"),
                Objects.requireNonNull(role.getId(), "role id must not be null")
        );
    }
}
