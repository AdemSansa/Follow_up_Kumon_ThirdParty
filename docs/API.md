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

## Parents

```http
GET    /api/parents
GET    /api/parents/{id}
POST   /api/parents
PUT    /api/parents/{id}
```

## Follow-Up Plans

```http
GET    /api/follow-up-plans
GET    /api/follow-up-plans/{id}
POST   /api/follow-up-plans
PUT    /api/follow-up-plans/{id}
DELETE /api/follow-up-plans/{id}
```

## Follow-Ups

```http
GET    /api/follow-ups
GET    /api/follow-ups/{id}
POST   /api/follow-ups
PUT    /api/follow-ups/{id}
POST   /api/follow-ups/{id}/complete
POST   /api/follow-ups/{id}/reschedule
POST   /api/follow-ups/{id}/cancel
```

### Check-in completion payload

`POST /api/follow-ups/{id}/complete` — the core automation trigger, must run
transactionally end-to-end (validate → save ratings/notes/concerns → create
action items → mark COMPLETED → compute + create next occurrence → create
calendar event → schedule reminders → update statistics → publish realtime
event).

```json
{
  "progressRating": 4,
  "engagementRating": 5,
  "parentSatisfactionRating": 4,
  "concerns": ["HOMEWORK"],
  "summary": "Good progress this month.",
  "recommendations": "Continue regular practice.",
  "actionItems": [
    { "title": "Monitor homework", "dueDate": "2026-10-01" }
  ]
}
```

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

## Conventions (to establish in Sprint 1)

- JSON request/response bodies, ISO-8601 dates/times.
- JWT bearer auth on all endpoints except `/api/auth/login`,
  `/api/auth/refresh`, and the `/api/scheduling/{token}/*` parent-facing
  endpoints (token-scoped instead, see above).
- Standard error envelope + validation error format — to be defined with
  the global exception handler in Sprint 1.
- Pagination/filtering params for list endpoints — to be defined in Sprint 2
  (students/parents) and reused elsewhere.
