# Backend

Spring Boot 4 (Java 21) modular monolith. See
[../docs/ARCHITECTURE.md](../docs/ARCHITECTURE.md) for module layout and
[../docs/API.md](../docs/API.md) / [../docs/DATABASE.md](../docs/DATABASE.md)
for the API and schema.

## Run locally

1. Start the dev database (from the repo root):

   ```bash
   cp .env.example .env
   docker compose up -d
   ```

2. Run the app, picking up `.env` values as environment variables (or export
   them yourself / use your IDE's run configuration):

   ```bash
   ./mvnw spring-boot:run
   ```

3. On first startup, if the `users` table is empty, a bootstrap admin
   account is created automatically from `ADMIN_EMAIL` / `ADMIN_PASSWORD`
   (defaults: `admin@example.com` / `changeme`). Change its password after
   first login — there's no self-service password change endpoint yet, so
   for now that means updating the user directly.

   ```bash
   curl -X POST http://localhost:8080/api/auth/login \
     -H "Content-Type: application/json" \
     -d '{"email":"admin@example.com","password":"changeme"}'
   ```

## Test

```bash
./mvnw test
```

Most tests (JWT service, auth controller slice) run without a database.
`FollowupApplicationTests` (`@SpringBootTest`) loads the full context —
Flyway, JPA, Security — and needs the dev database from step 1 running.

## Auth module (Sprint 1)

- Stateless JWT (`Authorization: Bearer <token>`), no server-side session or
  refresh-token store.
- `POST /api/auth/login` — public.
- `POST /api/auth/refresh` — requires a currently-valid bearer token, issues
  a new one.
- `POST /api/auth/logout` — no-op server-side (nothing to invalidate
  statelessly); exists so the frontend has a symmetric call while it
  discards its local token.
- Roles: `ADMIN` (Administrator/Director), `STAFF` (Teacher/Follow-Up
  Staff) — see [../docs/PROJECT_SCOPE.md](../docs/PROJECT_SCOPE.md).
- `/api/scheduling/**` is intentionally public/tokenless (parent
  slot-scheduling links) — see
  [../docs/features/parent-slot-scheduling.md](../docs/features/parent-slot-scheduling.md).
