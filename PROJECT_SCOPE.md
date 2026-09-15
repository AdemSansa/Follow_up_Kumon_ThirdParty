# Project Scope

Condensed reference derived from [student_follow_up_app_full_plan.md](./student_follow_up_app_full_plan.md).
That document is the source of truth for full detail — this file exists for
quick lookup during implementation.

## Core concept

```text
Student → Follow-Up Plan (recurrence rule) → Follow-Up Instances (actual events)
```

- **Follow-Up Plan** = the rule (frequency, day/time, staff, parent, duration).
- **Follow-Up** = one concrete scheduled/completed occurrence.
- Rescheduling or completing an instance never mutates the plan's rule.

## Primary objectives (see plan §2)

Centralize student data, identify students needing follow-up, configure
per-student frequency, auto-generate future follow-ups, schedule meetings,
integrate calendar, send reminders, make check-in fast (30–90s), store
structured notes, track history, provide live statistics, detect
overdue/missed follow-ups, reduce admin work.

## User roles (see plan §4)

- **Administrator/Director** — full access.
- **Teacher/Follow-Up Staff** — assigned students, check-ins, notes,
  rescheduling. Permissions configurable later.

## Modules (see plan §5)

Dashboard, Students, Parents, Follow-Up, Calendar, Check-In, Notes,
Statistics, Notifications, Settings.

## MVP boundary (Phase 1 — plan §47)

Build only:

```text
Authentication
Students
Parents
Follow-Up Plans
Follow-Up Instances
Calendar (internal)
Check-In
Notes
Dashboard
Basic Statistics
```

Automation in MVP: recurring follow-up generation, reminders, overdue
detection, automatic next-appointment creation.

**Explicitly deferred** to Phase 2 (Google/Outlook calendar, email
notifications, parent communication, action items, advanced stats, progress
graphs, audit logs) and Phase 3 (WhatsApp/SMS, AI features). Do not build
calendar sync or AI before the core workflow is solid — see plan §76 build
order.

## Non-negotiable business rules (plan §74)

1. Historical follow-ups are never deleted.
2. Deactivating a plan does not delete history.
3. Rescheduling one occurrence does not change the recurring plan.
4. Completing a follow-up automatically determines the next occurrence.
5. A paused student receives no new automatic follow-ups.
6. Cancelled meetings are never treated as completed.
7. Overdue follow-ups must be visible to management.
8. Only authorized staff can view/edit student information.
9. Parent-facing summaries never auto-expose internal notes.
10. Calendar sync (Google/Outlook) is never the source of truth — the
    database is.

## Definition of done (plan §73)

Per feature: DB model, migration, entity, repository, service, controller,
validation, authorization, unit tests, frontend service, frontend UI,
loading/error/empty states, responsive design, docs.
