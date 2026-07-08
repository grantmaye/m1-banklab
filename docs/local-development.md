# Local Development

## Prerequisites

- Java 21
- Maven
- Docker Desktop

Check versions:

```sh
java -version
mvn -version
docker --version
```

## Environment

Copy the sample environment file:

```sh
cp .env.example .env
```

Default values:

```text
POSTGRES_DB=m1_banklab
POSTGRES_USER=m1_banklab
POSTGRES_PASSWORD=m1_banklab
POSTGRES_PORT=5432
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/m1_banklab
SPRING_DATASOURCE_USERNAME=m1_banklab
SPRING_DATASOURCE_PASSWORD=m1_banklab
SERVER_PORT=8080
```

## Start PostgreSQL

```sh
docker compose up -d
```

If port `5432` is already occupied:

```sh
POSTGRES_PORT=55432 docker compose up -d
```

Then run the app with:

```sh
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:55432/m1_banklab mvn spring-boot:run
```

## Run Tests

```sh
mvn test
```

Tests include unit tests and a Testcontainers-backed PostgreSQL integration test. If local Docker Desktop is not discoverable by Testcontainers, the integration test is configured to skip locally. GitHub Actions runs it against a standard Ubuntu Docker daemon.

## Run The App

```sh
mvn spring-boot:run
```

Swagger UI:

```text
http://localhost:8080/swagger-ui.html
```

OpenAPI JSON:

```text
http://localhost:8080/v3/api-docs
```

## Stop Local Infrastructure

```sh
docker compose down
```

To remove the Postgres volume:

```sh
docker compose down -v
```
