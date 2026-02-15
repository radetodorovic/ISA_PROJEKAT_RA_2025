# Repository Guidelines

## Project Structure and Module Organization
- `src/main/java/com/isa/backend/` contains Spring Boot code (controllers, services, repositories, models).
- `src/main/resources/` holds application configuration and static assets.
- `src/test/java/com/isa/backend/` contains tests; current tests live under `service/`.
- `docker-compose.yml`, `Dockerfile`, `prometheus/`, and `grafana/` support local infra and monitoring.
- `load-tests/` and `k6/` contain load testing artifacts.
- `uploads/` is used for runtime file storage.

## Build, Test, and Development Commands
- `mvn spring-boot:run` starts the API locally with Spring Boot.
- `mvn test` runs the JUnit test suite.
- `mvn -DskipTests package` builds a jar without tests (also used by `install-and-build.ps1`).
- `.\install-and-build.ps1` downloads Maven locally if needed and builds.
- `docker compose up --build` runs API plus Postgres, RabbitMQ, Traefik, and monitoring stack.

## Coding Style and Naming Conventions
- Java: 4-space indentation, one class per file, `PascalCase` class names, `camelCase` methods/fields.
- Packages follow `com.isa.backend.<module>` naming.
- Keep controllers thin and push logic into services; prefer DTOs at API boundaries.
- Use Lombok annotations where already present to reduce boilerplate.

## Testing Guidelines
- Framework: Spring Boot Test (JUnit 5).
- Test names use `*Test` suffix (e.g., `VideoViewCountConcurrencyTest`).
- Add new tests under `src/test/java/com/isa/backend/` mirroring the main package layout.
- Run focused tests with `mvn -Dtest=ClassName test`.

## Commit and Pull Request Guidelines
- Recent commits are short summaries and sometimes prefixed with a type (e.g., `feat:`); others are informal.
- Prefer consistent, imperative messages: `feat: add video metrics` or `fix: handle null tags`.
- PRs should include: what changed, why, how to test, and any screenshots/logs for behavior changes.
- Link related issues if available; call out any config or data migrations.

## Configuration and Security Notes
- Local DB and RabbitMQ are configured in `docker-compose.yml`.
- Do not commit secrets; use `src/main/resources/` config placeholders and environment overrides.
