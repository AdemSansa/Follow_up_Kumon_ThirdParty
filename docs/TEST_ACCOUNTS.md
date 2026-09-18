# Test Accounts (local dev only)

These accounts are created automatically by `AdminBootstrap`
(`backend/src/main/java/com/school/auth/AdminBootstrap.java`) the first
time the backend starts against an **empty** `users` table — i.e. a fresh
dev database. If the table already has any user, bootstrap does nothing
(it never overwrites or resets existing accounts).

| Email | Password | Role | Notes |
|---|---|---|---|
| `admin@example.com` | `changeme` | `ADMIN` | Administrator/Director — full access per docs/PROJECT_SCOPE.md |
| `staff@example.com` | `changeme` | `STAFF` | Teacher/Follow-Up Staff |

Both are controllable via environment variables (see `.env.example`):

```env
ADMIN_EMAIL=admin@example.com
ADMIN_PASSWORD=changeme
STAFF_EMAIL=staff@example.com
STAFF_PASSWORD=changeme
```

## Logging in

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@example.com","password":"changeme"}'
```

Or sign in through the frontend at `http://localhost:4200/login`.

## Role enforcement status

As of Sprint 3, **the backend does not yet enforce ADMIN vs STAFF
differences** — `SecurityConfig` only checks `anyRequest().authenticated()`,
with no `@PreAuthorize`/role-based access rules per endpoint, and the
frontend shows the same UI to both roles. These two accounts exist now so
that role-based access control (when it's built) has real accounts to
test against from day one, and so the login/JWT flow can be exercised as
both roles in the meantime.

## Not for production

These are placeholder credentials meant for a local dev database only.
Before any non-local deployment: change `ADMIN_PASSWORD`/`STAFF_PASSWORD`
(or better, disable bootstrap and create real accounts through whatever
user-management flow exists by then), and never commit a real `.env` with
production credentials — see `.gitignore`.
