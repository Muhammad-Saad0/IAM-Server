# Contributing Guide

This document defines the development workflow and contribution rules for this project.

## Branch Naming

Create a new branch for every feature, bug fix, refactor, or documentation change.

Use this format:

```text
<type>/<short-description>
```

Examples:

```text
feature/user-registration
feature/jwt-login
feature/refresh-token-flow
bugfix/invalid-login-response
refactor/security-config
docs/update-local-setup
chore/add-docker-compose
test/add-auth-service-tests
```

Branch names should be lowercase and use hyphens instead of spaces.

Good:

```text
feature/user-registration
bugfix/token-expiry-validation
refactor/auth-service
```

Bad:

```text
Feature/UserRegistration
my-branch
final-code
changes
```

## Commit Message Convention

Use clear and meaningful commit messages.

Use this format:

```text
<type>: <short message>
```

Allowed commit types:

```text
feat      new feature
fix       bug fix
docs      documentation changes
style     formatting changes only
refactor  code change that does not change behavior
test      adding or updating tests
chore     build, config, or tooling changes
```

Examples:

```text
feat: add user registration endpoint
feat: implement jwt login
fix: handle duplicate email during registration
fix: return proper error for invalid credentials
refactor: move token generation to jwt service
docs: add local setup instructions
chore: configure postgres docker compose
test: add auth service unit tests
```

## Main Branch Rules

The `main` branch should always contain stable code.

Do not commit directly to `main`.

Create a separate branch, complete the work, and merge it through a pull request.

## Database Migration Rules

Use Flyway for database schema changes.

Migration files should be placed inside:

```text
src/main/resources/db/migration/
```

Use this naming format:

```text
V<version>__<description>.sql
```

Examples:

```text
V1__create_users_table.sql
V2__create_roles_table.sql
V3__create_refresh_tokens_table.sql
```

Do not edit an already-applied migration file.

If a schema change is needed, create a new migration file.

These values should not be committed to Git.

## Package Structure

Use a feature-based package structure where possible.

Example:

```text
com.example.iam
├── auth
├── user
├── role
├── token
├── security
└── common
```

Each feature package can contain its own controllers, services, repositories, DTOs, and entities.

Example:

```text
auth/
├── AuthController.java
├── AuthService.java
├── LoginRequest.java
├── RegisterRequest.java
└── AuthResponse.java
```

## Testing Rules

Add tests for important business logic.

Examples:

```text
AuthService tests
JwtService tests
RefreshTokenService tests
User registration validation tests
Login validation tests
```

Before pushing code, run:

```bash
./mvnw test
```

## Local Development

To run the project locally:

```bash
./mvnw spring-boot:run
```

If the project uses Docker Compose for PostgreSQL:

```bash
docker compose up -d
```

Then start the Spring Boot application.