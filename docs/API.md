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
(`MATH`/`ENGLISH`), `search` (matches student first/last name, student
phone, or a linked parent's first/last name/phone), plus standard Spring
Data paging — `page`, `size`, `sort` (e.g. `sort=lastName,asc`; defaults to
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
