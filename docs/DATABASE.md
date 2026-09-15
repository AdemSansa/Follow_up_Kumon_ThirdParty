# Database Schema

PostgreSQL, managed via Flyway migrations. Full rationale in
[full-product-plan.md](./full-product-plan.md)
§9, §11, §15, §34, §39. This file tracks the schema as it's actually built;
keep it in sync with `backend/src/main/resources/db/migration`.

## Core tables (planned)

```text
users
students
parents
student_parents

follow_up_plans
follow_ups

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
enrollmentDate, status, level, subjects, assignedTeacher, parentIds,
notes, createdAt, updatedAt
```

Status: `ACTIVE | PAUSED | INACTIVE | COMPLETED | WITHDRAWN`

### Parent

```text
id, firstName, lastName, phone, email, relationship,
preferredContactMethod, notes
```

Contact method: `PHONE | EMAIL | SMS | WHATSAPP`

### FollowUpPlan

```text
id, studentId, frequencyType, interval, startDate, preferredDay,
preferredTime, meetingDuration, assignedStaffId, parentId, location,
meetingType, active, nextFollowUpDate, reminderSettings, createdAt,
updatedAt
```

Frequency options: Weekly, Every 2 Weeks, Monthly, Every 2 Months, Every 3
Months, Every 6 Months, Yearly, Custom (`Every [X] days/weeks/months`).

### FollowUp (instance)

```text
id, studentId, followUpPlanId, parentId, assignedStaffId, type,
scheduledStart, scheduledEnd, status, completedAt, completedBy, summary,
notes, progressRating, engagementRating, parentSatisfactionRating,
concerns, recommendations, actionItems, nextFollowUpDate, calendarEventId,
createdAt, updatedAt
```

Type: `PARENT_MEETING | PHONE_CALL | VIDEO_CALL | IN_PERSON |
EMAIL_CHECK_IN | INTERNAL_REVIEW`

Status: `SCHEDULED | CONFIRMED | COMPLETED | MISSED | CANCELLED |
RESCHEDULED` (consider adding `OVERDUE` — see ARCHITECTURE.md overdue logic)

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
`backend/src/main/resources/db/migration`. First migration (Sprint 1):
`users` + roles. Students/parents tables land in Sprint 2, follow-up
plan/instance tables in Sprints 3–4, etc., matching the build order in
ARCHITECTURE.md.
