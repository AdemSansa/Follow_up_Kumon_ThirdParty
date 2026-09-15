# Database Schema

PostgreSQL, managed via Flyway migrations. Full rationale in
[full-product-plan.md](./full-product-plan.md)
§9, §11, §15, §34, §39. This file tracks the schema as it's actually built;
keep it in sync with `backend/src/main/resources/db/migration`.

## Core tables

Built (V1–V2): `users`, `students`, `parents`, `student_parents`,
`student_subjects`. Planned (later sprints):

```text
follow_up_plans
follow_ups
follow_up_slot_options

notes
action_items

calendar_events
notifications

student_progress
audit_logs
```

## Relationships

```text
users
  │
  ├── assigned to ──┐
  ▼                  ▼
students ── follow_up_plans ── follow_ups
   │                               │
   │                               ├── notes
   │                               ├── actions
   │                               └── calendar_event
   │
   └── student_parents ── parents
```

## Entity field reference

### Student

```text
id, firstName, lastName, dateOfBirth, gender, phone, email, address,
enrollmentDate, status, level, subjects, assignedTeacherId, parents,
notes, createdAt, updatedAt
```

Status: `ACTIVE | PAUSED | INACTIVE | COMPLETED | WITHDRAWN`

`subjects` is a `Set<Subject>` (`MATH | ENGLISH`), stored in its own
`student_subjects(student_id, subject)` table (JPA `@ElementCollection`),
not a single column — this school only teaches those two subjects (see
docs/PROJECT_SCOPE.md), so a fixed enum was preferred over a free-text
list. `assignedTeacherId` is a plain UUID column referencing `users.id`
(validated in `StudentService`, not a DB foreign key with a JPA relation —
kept as a loose reference so the students module doesn't need to depend on
the auth module's entities). `parents` is a `@ManyToMany` via
`student_parents`; the API writes it as `parentIds` and reads it as nested
`ParentResponse` objects — see docs/API.md.

### Parent

```text
id, firstName, lastName, phone, email, relationship,
preferredContactMethod, notes
```

Contact method: `PHONE | EMAIL | SMS | WHATSAPP`. `relationship` is a free
string (e.g. "Mother", "Guardian") — the plan doesn't enumerate it, unlike
contact method.

### FollowUpPlan

```text
id, studentId, frequencyType, interval, startDate, preferredDay,
preferredTime, meetingDuration, assignedStaffId, parentId, location,
meetingType, active, nextFollowUpDate, reminderSettings, createdAt,
updatedAt,
schedulingMode, slotWindowOpenDays, slotOptionsCount, slotSelectionDeadlineHours
```

Frequency options: Weekly, Every 2 Weeks, Monthly, Every 2 Months, Every 3
Months, Every 6 Months, Yearly, Custom (`Every [X] days/weeks/months`).

`schedulingMode`: `AUTO_FIXED | PARENT_PICKS_SLOT` (default `AUTO_FIXED`) —
see [features/parent-slot-scheduling.md](./features/parent-slot-scheduling.md).

### FollowUp (instance)

```text
id, studentId, followUpPlanId, parentId, assignedStaffId, type,
scheduledStart, scheduledEnd, status, completedAt, completedBy, summary,
notes, progressRating, engagementRating, parentSatisfactionRating,
concerns, recommendations, actionItems, nextFollowUpDate, calendarEventId,
createdAt, updatedAt,
schedulingStatus, schedulingToken, schedulingTokenExpiresAt
```

Type: `PARENT_MEETING | PHONE_CALL | VIDEO_CALL | IN_PERSON |
EMAIL_CHECK_IN | INTERNAL_REVIEW`

Status: `SCHEDULED | CONFIRMED | COMPLETED | MISSED | CANCELLED |
RESCHEDULED` (consider adding `OVERDUE` — see ARCHITECTURE.md overdue logic)

`schedulingStatus`: `NOT_REQUIRED | AWAITING_PARENT_SELECTION | CONFIRMED |
AUTO_CONFIRMED` — only meaningful when the plan's `schedulingMode` is
`PARENT_PICKS_SLOT`. `schedulingToken` is stored hashed, single-use. See
[features/parent-slot-scheduling.md](./features/parent-slot-scheduling.md).

### FollowUpSlotOption

Candidate time slots offered to the parent for one `FollowUp`; at most one
per follow-up has `isSelected = true`.

```text
id, followUpId, startTime, endTime, isSelected, createdAt
```

### ActionItem

```text
id, followUpId, studentId, assignedTo, title, description, dueDate,
priority, status, completedAt
```

Status: `TODO | IN_PROGRESS | DONE | CANCELLED`

### AuditLog

```text
id, userId, action, entityType, entityId, timestamp, metadata
```

Example actions: `STUDENT_CREATED`, `STUDENT_UPDATED`, `FOLLOWUP_CREATED`,
`FOLLOWUP_COMPLETED`, `FOLLOWUP_RESCHEDULED`, `NOTE_CREATED`,
`PARENT_UPDATED`.

### CalendarEvent

Stores internal event data plus external sync metadata (never the source of
truth once synced):

```text
externalProvider, externalEventId, externalCalendarId, syncStatus,
lastSyncedAt
```

## Migration convention

Flyway, versioned `V{n}__{description}.sql` under
`backend/src/main/resources/db/migration`. `V1` (Sprint 1): `users` +
roles. `V2` (Sprint 2): `students`, `parents`, `student_parents`,
`student_subjects`. Follow-up plan/instance tables land in Sprints 3–4,
etc., matching the build order in ARCHITECTURE.md.
