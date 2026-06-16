# Hexagonal Architecture

This project uses a pragmatic hexagonal architecture. We care about dependency direction and clean boundaries, not maximum ceremony.

The main rule:

```text
adapter -> application -> domain
```

## Package Shape

Use `com.example.iam` as the root package.

Organize by business capability first:

```text
com.example.iam
  IamServerApplication

  account
    domain
      model
      policy
      exception
    application
      command
      query
      port
        in
        out
      service
    adapter
      in
        web
      out
        persistence
        security
        messaging

  auth
    domain
    application
    adapter

  shared
    domain
    application
    adapter
```

Do not create empty folders upfront. Add packages only when code belongs there.

## Data Flow

Inbound flow:

```text
HTTP request / security event / message
  -> inbound adapter
  -> inbound port
  -> application service
  -> domain model / domain policy
  -> outbound port
  -> outbound adapter
  -> database / token library / email provider / external system
```

Example:

```text
RegisterUserController
  -> UserUseCase
  -> RegisterUserService
  -> User
  -> PasswordHasher
  -> UserPersistencePort
  -> JpaUserPersistenceAdapter
  -> JpaUserRepository
```

## Folders

`domain` contains business concepts and rules.

Examples:

```text
User
EmailAddress
PasswordPolicy
Role
Permission
AccountStatus
```

`application` contains use cases, commands, queries, services, and ports.

Examples:

```text
RegisterUserCommand
UserUseCase
RegisterUserService
UserPersistencePort
PasswordHasher
TokenIssuer
```

`adapter` contains Spring, HTTP, persistence, security, messaging, and provider code.

Examples:

```text
RegisterUserController
RegisterUserRequest
RegisterUserResponse
JpaUserPersistenceAdapter
JpaUserRepository
BCryptPasswordHasher
JwtTokenIssuer
```

## Domain Entities

We will use shared domain/JPA entities by default.

That means this is acceptable:

```java
package com.example.iam.account.domain.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "users")
public class User {
    @Id
    private UUID id;

    private String email;

    protected User() {
        // Required by JPA.
    }

    public boolean canLogin() {
        return true;
    }
}
```

Do not create a duplicate `UserJpaEntity` unless the persistence shape genuinely differs from the domain shape.

Keep these out of domain:

```text
Spring Data repositories
EntityManager
controllers
request/response DTOs
security filters
token provider clients
email provider clients
```

## Lombok and JPA Entities

Use Lombok to reduce safe boilerplate in JPA entities.

Preferred:

```java
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
```

Do not use Lombok `@Data` on entities. It generates broad setters plus `equals`, `hashCode`, and `toString`, which are risky around mutable JPA state and lazy relationships.

Keep domain behavior explicit:

```java
user.lock(now);
user.disable(now);
```

## Ports

Inbound ports describe what the application can do.

Use one inbound port per cohesive capability instead of one interface per operation. For account/user behavior, prefer `UserUseCase` with multiple user operations:

```java
public interface UserUseCase {
    RegisteredUser register(RegisterUserCommand command);
    UserProfile getProfile(GetUserProfileQuery query);
    void deactivate(DeactivateUserCommand command);
}
```

Outbound ports describe what the application needs from the outside:

```java
public interface UserPersistencePort {
    boolean existsByEmail(EmailAddress email);
    User save(User user);
}
```

Name outbound ports by capability, not by framework.

Use:

```text
UserPersistencePort
PasswordHasher
TokenIssuer
TokenVerifier
EmailSender
AuditEventPublisher
```

Avoid using `Repository` for application ports. Reserve repository names for Spring Data interfaces inside persistence adapters:

```text
adapter/out/persistence/JpaUserRepository.java
```

## Adapter Rules

Inbound adapters:

```text
adapter/in/web
adapter/in/messaging
adapter/in/security
```

Responsibilities:

```text
validate transport shape
map request DTOs to application commands
call inbound ports
map application results to responses
```

Outbound adapters:

```text
adapter/out/persistence
adapter/out/security
adapter/out/messaging
adapter/out/email
```

Responsibilities:

```text
implement outbound ports
call Spring Data repositories or provider clients
hide infrastructure details from application services
return domain objects or application result records
```

## DTO Policy

Use separate models only when they protect a real boundary.

Recommended:

```text
HTTP request/response DTOs  -> adapter/in/web
commands                    -> application/command
queries                     -> application/query
domain/JPA entities         -> domain/model
provider DTOs               -> adapter/out/<provider>
```

Avoid:

```text
request DTO -> application service parameter
Spring Data repository -> application service
JPA repository -> controller
provider DTO -> domain field
```

## Transactions

Application services own transaction boundaries:

```java
@Service
public class RegisterUserService implements UserUseCase {
    @Transactional
    public RegisteredUser register(RegisterUserCommand command) {
        // use case workflow
    }

    public UserProfile getProfile(GetUserProfileQuery query) {
        // query workflow
    }

    @Transactional
    public void deactivate(DeactivateUserCommand command) {
        // use case workflow
    }
}
```

Controllers should not own transactions. Domain objects should not know transactions exist.

## Testing

Default testing approach:

```text
domain tests                -> no Spring
application service tests   -> fake outbound ports
adapter tests               -> Spring MVC, Spring Security, JPA, Testcontainers as needed
full context tests          -> few
```
