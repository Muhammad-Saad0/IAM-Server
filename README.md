# IAM-Server

## Local development

Start the local PostgreSQL database:

```bash
docker compose up -d
```

Generate local OAuth/OIDC RSA keys. These are development keys only and should
not be committed:

```bash
openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 -out /tmp/iam-oauth-private.pem
openssl rsa -pubout -in /tmp/iam-oauth-private.pem -out /tmp/iam-oauth-public.pem
```

Create a local `.env` file:

```bash
OAUTH2_ISSUER=http://localhost:8080
OAUTH2_JWK_PRIVATE_KEY="$(cat /tmp/iam-oauth-private.pem)"
OAUTH2_JWK_PUBLIC_KEY="$(cat /tmp/iam-oauth-public.pem)"
```

Run the Spring Boot app locally. The script loads `.env` and then starts the app:

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
