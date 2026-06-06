package com.example.iam.auth.domain.model;

import com.example.iam.account.domain.model.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "auth_events")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AuthEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /*
     * user_id is nullable in the database. Failed login attempts may only have
     * a subject such as the attempted email, not a resolved user account.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 100)
    private AuthEventType eventType;

    /*
     * The identifier the request claimed to be about. Successful events can
     * store the resolved user's email; failed logins can store the attempted
     * email even when no user row exists.
     */
    @Column(length = 320)
    private String subject;

    /*
     * The browser/client string from the HTTP request, useful for audit review
     * and spotting suspicious client patterns without storing network address.
     */
    @Column(name = "user_agent", length = 512)
    private String userAgent;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    public AuthEvent(User user, AuthEventType eventType, String subject, String userAgent, Instant occurredAt) {
        this.user = user;
        this.eventType = Objects.requireNonNull(eventType, "eventType must not be null");
        this.subject = subject;
        this.userAgent = userAgent;
        this.occurredAt = Objects.requireNonNull(occurredAt, "occurredAt must not be null");
    }

    /*
     * Successful authentication has a resolved user. The subject is still kept
     * as a denormalized audit value so events remain readable if user data
     * changes later.
     */
    public static AuthEvent loginSuccess(User user, String userAgent, Instant now) {
        Objects.requireNonNull(user, "user must not be null");
        return new AuthEvent(user, AuthEventType.LOGIN_SUCCESS, user.getEmail(), userAgent, now);
    }

    /*
     * Failed authentication may not resolve to a user row, so it records the
     * attempted subject instead of requiring a user.
     */
    public static AuthEvent loginFailure(String subject, String userAgent, Instant now) {
        return new AuthEvent(null, AuthEventType.LOGIN_FAILURE, subject, userAgent, now);
    }
}
