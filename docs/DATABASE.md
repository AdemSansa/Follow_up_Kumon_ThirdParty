# Database Schema

PostgreSQL, managed via Flyway migrations. Full rationale in
[full-product-plan.md](./full-product-plan.md)
§9, §11, §15, §34, §39. This file tracks the schema as it's actually built;
keep it in sync with `backend/src/main/resources/db/migration`.

## Core tables

Built: `users` (V1); `students`, `parents`, `student_parents`,
`student_subjects` (V2); `follow_up_plans`, `follow_up_plan_reminders`
(V3); `follow_ups` (V4). Planned (later sprints):

```text
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

### FollowUpPlan (Sprint 3)

```text
id, student, parent, assignedStaffId, frequencyType, customIntervalValue,
customIntervalUnit, startDate, preferredDay, preferredTime,
meetingDurationMinutes, location, meetingType, active, nextFollowUpDate,
reminderOffsets, createdAt, updatedAt
```

Frequency options (`frequencyType`): `WEEKLY | EVERY_2_WEEKS | MONTHLY |
EVERY_2_MONTHS | EVERY_3_MONTHS | EVERY_6_MONTHS | YEARLY | CUSTOM` — for
`CUSTOM`, `customIntervalValue` + `customIntervalUnit`
(`DAYS | WEEKS | MONTHS`) are both required (validated in
`FollowUpPlanService`). `reminderOffsets` is a `Set<ReminderOffset>`
(`SEVEN_DAYS_BEFORE | TWENTY_FOUR_HOURS_BEFORE | TWO_HOURS_BEFORE |
THIRTY_MINUTES_BEFORE`), stored in `follow_up_plan_reminders` — not
actually sent anywhere yet (no notification system exists; that's Sprint
9). `nextFollowUpDate` is computed and kept up to date by
`FollowUpGenerationService`, not set by the client.

**Not yet added** (deferred until actually built, per the "no speculative
columns" approach used throughout this schema): the parent
slot-scheduling fields (`schedulingMode`, `slotWindowOpenDays`,
`slotOptionsCount`, `slotSelectionDeadlineHours`) from
[features/parent-slot-scheduling.md](./features/parent-slot-scheduling.md).

### FollowUp (instance, Sprint 4)

One concrete scheduled occurrence, generated from a `FollowUpPlan` by
`FollowUpGenerationService` — see "Automation engine" in
[ARCHITECTURE.md](./ARCHITECTURE.md).

```text
id, student, followUpPlan, parent, assignedStaffId, type, scheduledStart,
scheduledEnd, status, createdAt, updatedAt
```

`followUpPlan` is nullable — an ad-hoc follow-up need not come from a
recurring plan. Type: `PARENT_MEETING | PHONE_CALL | VIDEO_CALL |
IN_PERSON | EMAIL_CHECK_IN | INTERNAL_REVIEW` (copied from the plan at
generation time, or given directly for an ad-hoc one).

Status: `SCHEDULED | CONFIRMED | COMPLETED | MISSED | CANCELLED |
RESCHEDULED`. No stored `OVERDUE` status — "overdue" is computed at query
time (`scheduledEnd < now` and an unresolved status), not persisted; see
"Overdue logic" in [ARCHITECTURE.md](./ARCHITECTURE.md) for why (no cron
job exists yet to keep a stored status current).

**Check-in fields (Sprint 6, migration V5)**: `completedAt`, `completedBy`,
`summary` (the quick note), `recommendations`, `progressRating`,
`engagementRating`, `parentSatisfactionRating` (each 1–5, enforced by a
`CHECK` constraint), and `concerns` — an `@ElementCollection` of the
`Concern` enum (`ATTENDANCE | HOMEWORK | MOTIVATION | PROGRESS |
BEHAVIOR`) stored in `follow_up_concerns`; "None" is simply an empty set.
All are null/empty until the follow-up is `COMPLETED`.

**Not yet added**: `notes` (reserved), `calendarEventId` — Sprint 11. The parent slot-scheduling
fields (`schedulingStatus`, `schedulingToken`,
`schedulingTokenExpiresAt`) — same feature doc as above, not built.

`student`/`parent`/`followUpPlan` on both entities are `EAGER`-fetched —
see "Entity association fetching" in
[ARCHITECTURE.md](./ARCHITECTURE.md); this is a project-wide convention,
not specific to these two entities.

### FollowUpSlotOption

Candidate time slots offered to the parent for one `FollowUp`; at most one
per follow-up has `isSelected = true`.

```text
id, followUpId, startTime, endTime, isSelected, createdAt
```

### ActionItem (Sprint 6, migration V5)

```text
id, followUpId, studentId, assignedTo, title, description, dueDate,
priority, status, completedAt, createdAt
```

Status: `TODO | IN_PROGRESS | DONE | CANCELLED`. Priority: `LOW | MEDIUM |
HIGH`. `assignedTo` is a nullable user id. `followUp` and `student` are
`EAGER` (project convention).

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
`student_subjects`. `V3` (Sprint 3): `follow_up_plans`,
`follow_up_plan_reminders`. `V4` (Sprint 4): `follow_ups`. Next up:
notes/actions/calendar tables in Sprints 5–6, matching the build order in
ARCHITECTURE.md.
