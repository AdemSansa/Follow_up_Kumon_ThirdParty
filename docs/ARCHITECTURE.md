# Architecture

Full rationale in [full-product-plan.md](./full-product-plan.md)
§29–33, §40, §43–44. This file is the quick-reference version.

## System overview

```text
Angular Frontend
      │
   REST / WS
      │
Spring Boot Backend (modular monolith)
      │
  ┌───┼────────────┬────────────────┐
  ▼   ▼             ▼                ▼
PostgreSQL  Scheduler/Jobs   Notification Service
```

Modular monolith first — **no microservices** until there's a proven reason.

## Backend module layout

```text
backend/src/main/java/com/school/
├── auth/           controller, service, repository, model
├── students/        "
├── parents/          "
├── followup/        controller, service, repository, scheduler, model
├── calendar/
├── notifications/
├── notes/
├── actions/
├── statistics/
└── common/
```

Controllers call services; controllers hold no scheduling/business logic.

## Frontend module layout

```text
frontend/src/app/
├── core/             auth, guards, interceptors, services
├── shared/           components, dialogs, tables, pipes
├── features/         dashboard, students, parents, followups, calendar,
│                      checkins, notes, actions, statistics, settings
└── layout/           sidebar, navbar, shell
```

## Automation engine

```text
FollowUpAutomationService
  generateUpcomingFollowUps()
  scheduleReminder()
  detectOverdueFollowUps()
  createCalendarEvent()
  rescheduleCalendarEvent()
  createNextOccurrence()
```

Daily job (Spring Scheduler in MVP; queue/job system later if needed):

1. Find active follow-up plans.
2. Generate missing upcoming follow-ups (rolling window, e.g. next 3–6 months
   — never generate years at once).
3. Find tomorrow's meetings → send reminders.
4. Find overdue follow-ups → mark overdue.
5. Generate dashboard alerts.
6. Sync calendar changes.

## Calendar sync strategy

Adapter pattern; the database is always authoritative, never the external
calendar.

```text
CalendarService
  ├── GoogleCalendarAdapter
  └── OutlookCalendarAdapter

interface: createEvent() / updateEvent() / deleteEvent() / getEvent()
```

Store per event: `externalProvider`, `externalEventId`, `externalCalendarId`,
`syncStatus`, `lastSyncedAt`.

## Notification architecture

```text
NotificationService
  sendFollowUpReminder()
  sendMeetingConfirmation()
  sendRescheduleNotification()
  sendFollowUpSummary()
```

Channels: In-App, Email (MVP+Phase 2 start), SMS/WhatsApp (Phase 3).

## Recurrence engine edge cases to handle explicitly

- Month-end (`LAST_DAY_OF_MONTH` vs `SAME_DAY_WHEN_POSSIBLE`).
- Holidays (future rule support).
- Rescheduling one occurrence never mutates the plan.
- Pausing a student pauses future generation.
- Deactivating a plan cancels/archives future pending occurrences; history
  stays.

## Overdue logic

```text
scheduledEnd < now AND status NOT IN (COMPLETED, CANCELLED) → OVERDUE
```

Distinguish `MISSED` (appointment actually missed) from `OVERDUE` (staff
hasn't completed it yet) if management needs that granularity.

## Security baseline

JWT auth, role-based authorization, password hashing, HTTPS, input
validation, DB constraints, audit logging, rate limiting, secure headers.
Never expose secrets/credentials to the frontend.

## Live updates

WebSocket for dashboard live updates in the target architecture; MVP may use
polling/API refresh instead and upgrade later. Events: `FOLLOWUP_CREATED`,
`FOLLOWUP_COMPLETED`, `FOLLOWUP_RESCHEDULED`, `FOLLOWUP_CANCELLED`,
`ACTION_CREATED`, `ACTION_COMPLETED`, `STUDENT_UPDATED`.

## Build order

See plan §76 for the full 18-step order. Do not start with calendar sync or
AI. Sequence: setup → auth → students → parents → follow-up plans →
follow-up instances → recurrence engine → check-in → notes/actions →
internal calendar → dashboard → statistics → notifications → automation →
Google Calendar → Outlook → advanced reporting → AI.
