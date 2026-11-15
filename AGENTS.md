# resume-api — Agent Instructions

## What this is
Java Spring Boot Modulith monolith — one of 5 repos in Shaahil's portfolio rebuild
(`frontend`, `resume-api`, `agent-service`, `pitcher`, `infra`). This service owns:
`auth` (OAuth2 Authorization Server), `ai` (RAG "chat with my resume"), `contact`,
`notification`, `analytics` (AI cost/token/latency metrics).

## Stack
- Java 21 (pom target), Spring Boot 4.1.1, Spring Modulith 2.1.1
- Spring Data JPA + PostgreSQL (pgvector image), Spring Security, Spring Authorization Server
- Build: Maven (`mvn clean install`)
- Local infra lives in `../infra/docker-compose.yml` — Postgres is on **host port 5433**
  (mapped from the container's internal 5432), plus Mongo, Redis, Kafka+Zookeeper.

## Module structure (Spring Modulith)
Five modules under `com.shaahil.resumeapi`: `auth`, `ai`, `contact`, `notification`, `analytics`.
- Each module has a `package-info.java` with `@org.springframework.modulith.ApplicationModule(displayName = "...")`.
- Cross-module calls are restricted to whatever a module's `allowedDependencies` permits
  (currently unrestricted while scaffolding — will be locked down module by module as the
  design solidifies).
- `ModularityTests` (in `src/test/java/com/shaahil/resumeapi/`) runs
  `ApplicationModules.of(ResumeApiApplication.class).verify()` — run this after any change
  to package structure or new cross-module calls.
- Never put a JUnit / spring-modulith-test class under `src/main/java` — main and test have
  separate classpaths, so test-scoped dependencies aren't visible to main compilation. This
  has caused a real build failure here before.

## Config / secrets
- `src/main/resources/application.yml` reads `${DB_USERNAME}` / `${DB_PASSWORD}` with
  **no defaults**. Real values are supplied via IntelliJ run-config environment variables
  or shell exports — never hardcode a fallback credential in a file that gets committed.
- Postgres JDBC URL is `jdbc:postgresql://localhost:5433/resume_db` — note the 5433,
  not the Postgres-default 5432 (that's the docker-compose host-port mapping, not the
  container's internal port).

## Auth design (already decided — don't relitigate without asking)
The `auth` module implements its own OAuth2 Authorization Server (via Spring Authorization
Server) issuing JWT access tokens through the Authorization Code flow. This is deliberate —
not federating login to an external IdP (Auth0/Keycloak/Cognito) — chosen specifically for
hands-on OAuth2 implementation depth ahead of interviews.

## Working style
Don't silently make architectural or
dependency decisions on my behalf — flag trade-offs and ask when something is a real fork
in the road.

## Git workflow
`main` → `release` → `feature/*` branches. No direct commits to `main` or `release` —
everything goes through a feature branch and a PR.
