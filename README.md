# Student Follow-Up & Check-In Management System

Private-school management application for a Kumon-style accelerated learning
center (Math & English). Automates recurring parent follow-ups: scheduling,
reminders, fast check-ins, notes, statistics, and calendar management.

Full product scope and roadmap: [student_follow_up_app_full_plan.md](./student_follow_up_app_full_plan.md)

## Documentation

- [PROJECT_SCOPE.md](./PROJECT_SCOPE.md) — objectives, modules, MVP boundaries
- [ARCHITECTURE.md](./ARCHITECTURE.md) — system architecture, backend/frontend structure
- [API.md](./API.md) — REST API reference
- [DATABASE.md](./DATABASE.md) — schema and entity relationships

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

Backend and frontend setup instructions will be added as each is scaffolded
(Sprint 1 and onward).

## Roadmap status

Currently: **Sprint 0 — Planning**. See the full plan for the sprint-by-sprint
roadmap (Sprints 0–12) and long-term phases.
