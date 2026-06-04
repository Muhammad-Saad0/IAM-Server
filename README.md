# IAM-Server

## Local development

Start the local PostgreSQL database:

```bash
docker compose up -d
```

Run the Spring Boot app locally:

```bash
./mvnw spring-boot:run
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
