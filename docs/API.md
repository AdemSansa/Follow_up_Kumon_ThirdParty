# API Reference

REST API. Full context in [full-product-plan.md](./full-product-plan.md)
§35–37. Endpoints will be expanded/refined during each sprint — this is the
planned MVP surface.

## Authentication

```http
POST /api/auth/login
POST /api/auth/logout
POST /api/auth/refresh
```

## Students

```http
GET    /api/students
GET    /api/students/{id}
POST   /api/students
PUT    /api/students/{id}
DELETE /api/students/{id}
```

`GET /api/students` query params: `status`, `assignedTeacherId`, `subject`
(`MATH`/`ENGLISH`), `parentId` (a parent's children — powers the "Add
child" list on a parent's page, see
[features/parent-first-registration.md](./features/parent-first-registration.md)),
`search` (matches student first/last name, student phone, or a linked
parent's first/last name/phone), plus standard Spring Data paging —
`page`, `size`, `sort` (e.g. `sort=lastName,asc`; defaults to
`size=20&sort=lastName`). Response is a `PageResponse` envelope: `{content,
page, size, totalElements, totalPages}` — see the error/page envelope note
under Conventions.

Request/response body includes `parentIds` (write) / `parents` (read,
nested `ParentResponse[]`) to link a student to its parent(s); an unknown
`parentIds` entry or `assignedTeacherId` is rejected with 400.

## Parents

```http
GET    /api/parents
GET    /api/parents/{id}
POST   /api/parents
PUT    /api/parents/{id}
```

`GET /api/parents` query params: `search` (first/last name, phone, email)
plus the same paging params as students.

## Follow-Up Plans (Sprint 3)

```http
GET    /api/follow-up-plans
GET    /api/follow-up-plans/{id}
POST   /api/follow-up-plans
PUT    /api/follow-up-plans/{id}
DELETE /api/follow-up-plans/{id}
```

`GET` query params: `studentId`, `active`, `assignedStaffId`, plus standard
paging.

Creating or updating a plan synchronously generates/regenerates its
`FollowUp` occurrences (see below) — no cron job exists yet (Sprint 7).
`DELETE` is rejected with 400 once a plan has any follow-up (any status):
deactivate it instead (`PUT` with `active: false`) — see business rule 1,
[PROJECT_SCOPE.md](./PROJECT_SCOPE.md). `parentId`, if set, must be one of
the student's own linked parents.

## Follow-Ups (Sprint 4 — the generated occurrences)

```http
GET    /api/follow-ups
GET    /api/follow-ups/{id}
POST   /api/follow-ups
POST   /api/follow-ups/{id}/reschedule
POST   /api/follow-ups/{id}/cancel
POST   /api/follow-ups/{id}/complete
```

No `PUT` (general edit) or `DELETE` — history is never rewritten wholesale
or hard-deleted (business rule 1); `reschedule`, `cancel` and `complete`
(the check-in flow, Sprint 6) are the only mutations.

`GET` query params: `studentId`, `followUpPlanId`, `status`,
`assignedStaffId`, `overdue` (boolean — computed at query time from
`scheduledEnd < now` and an unresolved status, not a stored status; see
"Overdue logic" in [ARCHITECTURE.md](./ARCHITECTURE.md)), `from`/`to`
(`YYYY-MM-DD`, inclusive calendar-day bounds — powers the calendar views,
Sprint 5; matches any follow-up whose `[scheduledStart, scheduledEnd)`
overlaps the range, not just ones that start inside it), plus standard
paging. Every response also includes a computed `overdue` boolean.

`POST /api/follow-ups` creates an ad-hoc follow-up (`followUpPlanId`
optional — not every follow-up has to come from a recurring plan).

`POST /api/follow-ups/{id}/reschedule` (body: `scheduledStart`,
`scheduledEnd`) moves one occurrence and sets its status to `RESCHEDULED`
— it never touches the plan (business rule 3). Rejected (400) if the
follow-up is already `CANCELLED` or `COMPLETED`.

`POST /api/follow-ups/{id}/cancel` sets status to `CANCELLED`; rejected
(400) if already `COMPLETED`.

### Check-in completion (Sprint 6)

`POST /api/follow-ups/{id}/complete` runs in one transaction: validate →
save ratings/notes/concerns → create action items → mark `COMPLETED`
(`completedAt`, `completedBy` = the calling user) → top up the plan's
rolling window of future occurrences. Rejected (400) if the follow-up is
already `COMPLETED` or `CANCELLED`, if a rating is outside 1–5 (the
`fieldErrors` entry names the field), or if an action item's `assignedTo`
is not a known user — in every failure case nothing is saved.

Not implemented yet from the plan's §37 chain: calendar event creation,
reminder scheduling, statistics updates and the realtime event. `notes`
on a follow-up is reserved; the quick note is stored as `summary`.

```json
{
  "progressRating": 4,
  "engagementRating": 5,
  "parentSatisfactionRating": 4,
  "concerns": ["HOMEWORK"],  // empty array = "None"
  "summary": "Good progress this month.",
  "recommendations": "Continue regular practice.",
  "actionItems": [
    { "title": "Monitor homework", "dueDate": "2026-10-01", "priority": "MEDIUM", "assignedTo": null }
  ]
}
```

Concerns: `ATTENDANCE | HOMEWORK | MOTIVATION | PROGRESS | BEHAVIOR`.

## Action Items (Sprint 6)

```http
GET  /api/action-items
POST /api/action-items/{id}/complete
```

`GET` filters: `studentId`, `followUpId`, `status`
(`TODO | IN_PROGRESS | DONE | CANCELLED`), `assignedTo`, plus paging. Items
are created only through the check-in payload above; `complete` marks one
`DONE` and stamps `completedAt`.

## Parent Slot Scheduling (public, tokenless)

```http
GET  /api/scheduling/{token}
POST /api/scheduling/{token}/select
```

No parent account, no JWT — see the "Conventions" note below and
[features/parent-slot-scheduling.md](./features/parent-slot-scheduling.md)
for the full flow and security notes.

## Notes

```http
GET    /api/students/{id}/notes
POST   /api/students/{id}/notes
PUT    /api/notes/{id}
DELETE /api/notes/{id}
```

## Statistics

```http
GET /api/statistics/dashboard
GET /api/statistics/students
GET /api/statistics/follow-ups
GET /api/statistics/progress
```

## Conventions

- JSON request/response bodies, ISO-8601 dates/times.
- JWT bearer auth (`Authorization: Bearer <token>`) on all endpoints except
  `/api/auth/login` and the `/api/scheduling/{token}/*` parent-facing
  endpoints (token-scoped instead, see above). `/api/auth/refresh` requires
  a currently-valid bearer token.
- Error envelope (from `GlobalExceptionHandler`, `backend/src/main/java/com/school/common/exception`):

  ```json
  {
    "timestamp": "2026-09-15T14:56:43.910338300Z",
    "status": 400,
    "error": "Validation Failed",
    "message": "One or more fields are invalid",
    "path": "/api/auth/login",
    "fieldErrors": [{ "field": "password", "message": "must not be blank" }]
  }
  ```

  `fieldErrors` is empty except for validation failures (400). Status codes
  in use: 400 (validation/bad request), 401 (missing/invalid/expired
  token, bad credentials), 403 (authenticated but not permitted), 404
  (not found), 405 (wrong HTTP method), 500 (unexpected — logged
  server-side, never leaks internal detail to the client).
- Pagination: standard Spring Data query params (`page`, `size`, `sort`),
  responses wrapped in the `PageResponse` envelope shown above (Students,
  Parents) — established in Sprint 2, reused by every later list endpoint.
