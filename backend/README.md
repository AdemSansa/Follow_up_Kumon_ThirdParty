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

3. On first startup, if the `users` table is empty, bootstrap ADMIN and
   STAFF accounts are created automatically — see
   [../docs/TEST_ACCOUNTS.md](../docs/TEST_ACCOUNTS.md) for credentials.
   Change their passwords after first login — there's no self-service
   password change endpoint yet, so for now that means updating the user
   directly.

   ```bash
   curl -X POST http://localhost:8080/api/auth/login \
     -H "Content-Type: application/json" \
     -d '{"email":"admin@example.com","password":"changeme"}'
   ```

## Test

```bash
./mvnw test
```

Most auth tests (JWT service, auth controller slice) and
`RecurrenceCalculatorTest` run without a database. The `@SpringBootTest`
classes (`FollowupApplicationTests`, `StudentParentIntegrationTest`,
`FollowUpPlanIntegrationTest`, `FollowUpEngineIntegrationTest`) load the
full context — Flyway, JPA, Security — and need the dev database from
step 1 running.

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

## Students & Parents module (Sprint 2)

- Full CRUD for both, plus student↔parent linking (`parentIds` on write,
  nested `parents` on read) and pagination/filtering — see
  [../docs/API.md](../docs/API.md).
- `assignedTeacherId` and `parentIds` are validated to reference real
  rows (400 if not); `subjects` is restricted to `MATH`/`ENGLISH`.
- All endpoints require a valid bearer token (no public student/parent
  data).

## Follow-Up Plans module (Sprint 3)

- `FollowUpPlan` is the recurrence **rule** (frequency, day/time, staff,
  parent, duration) — not a scheduled event. Generating actual
  occurrences from it is the Sprint 4 recurrence engine, not built yet.
- CRUD at `/api/follow-up-plans`, filterable by `studentId`, `active`,
  `assignedStaffId` — see [../docs/API.md](../docs/API.md).
- `parentId`, if set, must be one of the student's own linked parents
  (400 otherwise); `CUSTOM` frequency requires `customIntervalValue` +
  `customIntervalUnit`.
- `student`/`parent` associations are `EAGER` — see "Entity association
  fetching" in [../docs/ARCHITECTURE.md](../docs/ARCHITECTURE.md) for why
  (a real bug this sprint, only caught by manual testing — a
  `@Transactional` integration test doesn't catch it).
- `DELETE` is rejected (400) once a plan has any follow-up — deactivate
  (`PUT` with `active: false`) instead; see business rule 1.

## Follow-Up recurrence engine (Sprint 4)

- `RecurrenceCalculator` computes the Nth occurrence date for a plan by
  offsetting from the plan's own `startDate` (`startDate.plus(N *
  interval)`), never by chaining off the previously computed date — the
  latter drifts at month-end (Jan 31 → Feb 28 → **Mar 28**, wrong) where
  the former doesn't (Jan 31 → Feb 28 → **Mar 31**, correct). Same fix
  handles leap-year Feb 29 correctly. See the class Javadoc and
  `RecurrenceCalculatorTest` (12 cases, no DB needed).
- `FollowUpGenerationService.generateUpcoming()` fills in any missing
  occurrences up to a rolling 6-month window, runs synchronously on plan
  create/update (no cron job yet — Sprint 7), and skips entirely for a
  paused plan (`active: false`) or a non-`ACTIVE` student.
  `regenerateFuture()` (called on plan update) removes not-yet-happened
  `SCHEDULED` occurrences and regenerates them from the updated rule —
  `CONFIRMED`/`COMPLETED`/`MISSED`/`CANCELLED`/`RESCHEDULED` rows and
  anything already in the past are never touched.
- `POST /api/follow-ups/{id}/reschedule` and `.../cancel` — see
  [../docs/API.md](../docs/API.md). No `PUT`/`DELETE` on `FollowUp` by
  design (business rule 1).
- "Overdue" is computed per-response/per-query
  (`scheduledEnd < now` + unresolved status), not a stored status —
  avoids a stored value going stale with no scheduler to refresh it yet.

## Calendar (Sprint 5)

No `CalendarEvent` entity or table — the internal calendar (day/week/month
views) queries `FollowUp` directly via the new `from`/`to` date-range
filter on `GET /api/follow-ups` (`FollowUpSpecifications.overlapsRange`).
`calendar_events` (for external Google/Outlook sync metadata — Sprint 11)
is still just planned, per docs/DATABASE.md; there's no need for it until
external sync actually exists, and the internal calendar has no reason to
duplicate data `FollowUp` already has.

## Check-In & Action Items (Sprint 6)

- `CheckInService.complete()` is one `@Transactional` unit: it records the
  ratings/concerns/summary/recommendation on the `FollowUp`, saves any
  action items, marks it `COMPLETED`, then calls
  `FollowUpGenerationService.generateUpcoming()` so the plan's rolling
  window stays full. A failure anywhere (bad rating, unknown assignee,
  already completed/cancelled) leaves nothing saved.
- New `actions` package (`ActionItem` entity, repository + specifications,
  service, `GET /api/action-items`, `POST /api/action-items/{id}/complete`).
- Migration `V5__check_in_and_action_items.sql`. Covered by
  `CheckInIntegrationTest` (8 cases). See [../docs/API.md](../docs/API.md).
