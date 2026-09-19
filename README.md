# Student Follow-Up & Check-In Management System

Private-school management application for a Kumon-style accelerated learning
center (Math & English). Automates recurring parent follow-ups: scheduling,
reminders, fast check-ins, notes, statistics, and calendar management.

Full product scope and roadmap: [docs/full-product-plan.md](./docs/full-product-plan.md)

## Documentation

- [docs/PROJECT_SCOPE.md](./docs/PROJECT_SCOPE.md) — objectives, modules, MVP boundaries
- [docs/ARCHITECTURE.md](./docs/ARCHITECTURE.md) — system architecture, backend/frontend structure
- [docs/API.md](./docs/API.md) — REST API reference
- [docs/DATABASE.md](./docs/DATABASE.md) — schema and entity relationships
- [docs/TEST_ACCOUNTS.md](./docs/TEST_ACCOUNTS.md) — local dev login credentials

## Stack

- **Frontend**: Angular, TypeScript, Angular Material, RxJS
- **Backend**: Spring Boot, Java, Spring Security, Spring Data
- **Database**: PostgreSQL (Flyway migrations)
- **Infra (dev)**: Docker Compose

## Project layout

```text
backend/    Spring Boot modular monolith
frontend/   Angular application
docs/       supplementary documentation
```

## Getting started (dev)

Prerequisites: Docker, JDK 21+, Node.js 20+.

```bash
cp .env.example .env
docker compose up -d
```

See [backend/README.md](./backend/README.md) for running/testing the API,
and [frontend/README.md](./frontend/README.md) for the Angular app
(`cd frontend && npm install && ng serve`, backend must be running first).

## Roadmap status

Backend and frontend are in step through **Sprint 6**:

- **Sprint 1** — auth (JWT, roles)
- **Sprint 2** — Students & Parents (CRUD, linking, search/pagination),
  with a parent-first registration flow — see
  [docs/features/parent-first-registration.md](./docs/features/parent-first-registration.md)
- **Sprint 3** — Follow-Up Plans (the recurrence rule per student: CRUD,
  frequency/schedule, reminders)
- **Sprint 4** — the recurrence engine: turns a plan into actual scheduled
  `FollowUp` occurrences (drift-safe month-end/leap-year date math),
  overdue detection, reschedule, cancel
- **Sprint 5** — internal calendar (day/week/month views of every
  follow-up); no external Google/Outlook sync yet (Sprint 11)
- **Sprint 6** — the fast check-in flow: 1–5 star ratings, concern
  checkboxes, quick note, recommendation, optional action items, and a
  transactional `POST /api/follow-ups/{id}/complete`; action items are
  listed (and can be marked done) on the student page

Not yet built: Smart Check-In, calendar sync, notifications, and the daily
automation job that would otherwise keep occurrences generating without
someone editing a plan (Sprint 7). See the full plan for the
sprint-by-sprint roadmap (Sprints 0–12) and long-term phases.
