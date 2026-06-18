# IAM-Server

## Local development

Start the local PostgreSQL database:

```bash
docker compose up -d
```

Create a local `.env` file:

```bash
OAUTH2_ISSUER=http://localhost:8080
```

Run the Spring Boot app locally. The script loads `.env`, creates development-only
OAuth RSA keys under `secrets/` when they do not exist, and reuses those keys on
subsequent runs:

```bash
./scripts/dev-run
```

The app uses the local Docker database configured in `application.properties`:

```text
host: localhost
port: 5432
database: iam_server
user: iam_dev
password: iam_dev
```

Stop the database:

```bash
docker compose down
```

Stop the database and delete its stored data:

```bash
docker compose down -v
```
