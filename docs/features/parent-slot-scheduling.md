# Feature: Parent Slot Scheduling

Extension to the core scheduling flow described in
[../ARCHITECTURE.md](../ARCHITECTURE.md) and [../DATABASE.md](../DATABASE.md).
Added after Sprint 0 planning, before backend work started.

## Problem

The automation engine normally fixes each recurring follow-up at the plan's
`preferredDay`/`preferredTime`. That fixed slot won't always work for the
parent. Instead of forcing a reschedule after the fact, the parent should be
able to pick a convenient time from a small set of options when each
occurrence comes up.

## Design constraint

**No parent accounts.** Parents are not platform users and do not log in.
Scheduling access is a single-use, token-based link scoped to one follow-up
instance — effectively an "invite to pick a time," not a portal.

## Flow

```text
FollowUpPlan.schedulingMode = PARENT_PICKS_SLOT
        │
Automation engine generates upcoming FollowUp occurrence
        │
Instead of fixing scheduledStart/End immediately:
  - generate N candidate slots around preferredDay/preferredTime
  - create FollowUpSlotOption rows
  - generate a single-use schedulingToken (+ expiry)
  - FollowUp.schedulingStatus = AWAITING_PARENT_SELECTION
  - notify parent with link: /schedule/{token}
        │
Parent opens link (no login) → sees student name, plan context,
proposed slots → picks one
        │
POST /api/scheduling/{token}/select
  - validate token (not expired/used)
  - set FollowUp.scheduledStart/End from the chosen option
  - schedulingStatus = CONFIRMED, status = CONFIRMED
  - invalidate token
  - create calendar event
  - schedule reminders
        │
If no selection before the fallback deadline
(e.g. 24h before the earliest offered slot):
  - auto-confirm the plan's default preferredDay/preferredTime
  - schedulingStatus = AUTO_CONFIRMED
  - flag on staff dashboard ("attention required" list) so a human
    can double-check the parent didn't just miss the message
```

Plans that stay `AUTO_FIXED` are unaffected — this is opt-in per plan, not a
replacement for the default flow.

## Data model additions

### FollowUpPlan (new fields)

```text
schedulingMode        AUTO_FIXED | PARENT_PICKS_SLOT   (default AUTO_FIXED)
slotWindowOpenDays     how many days before the target date the window opens
slotOptionsCount        how many candidate slots to generate per occurrence
slotSelectionDeadlineHours   hours before the earliest slot to trigger fallback
```

### FollowUp (new fields)

```text
schedulingStatus   NOT_REQUIRED | AWAITING_PARENT_SELECTION | CONFIRMED | AUTO_CONFIRMED
schedulingToken            single-use random token (hashed at rest)
schedulingTokenExpiresAt
```

### New table: follow_up_slot_options

```text
FollowUpSlotOption
- id
- followUpId
- startTime
- endTime
- isSelected
- createdAt
```

One `FollowUp` has many `FollowUpSlotOption` rows (the candidates); at most
one is marked `isSelected`.

## API additions

Public endpoints — token-scoped, **not** behind JWT auth, since there's no
parent account:

```http
GET  /api/scheduling/{token}
POST /api/scheduling/{token}/select
```

`GET` returns only what's needed to render the picker: student first name,
follow-up type, the candidate slots, and token validity — never internal
notes, ratings, or other students' data.

`POST` body:

```json
{ "slotOptionId": "..." }
```

Rejects if the token is expired, already used, or the slot doesn't belong to
that token's follow-up.

## Security notes

- Token: cryptographically random (≥ 128 bits), stored hashed, single-use,
  time-limited.
- Rate-limit both endpoints (no auth to lean on).
- No enumeration: an invalid/expired/used token returns a generic 404/410,
  not a reason.
- This surface intentionally exposes minimal data — treat it as a public,
  unauthenticated endpoint from a security-review standpoint.

## Business rules added

- Parent slot selection never silently bypasses staff visibility — an
  auto-confirmed fallback always surfaces on the staff attention list.
- Rescheduling a slot-picked follow-up later still follows the existing rule
  (§ business rule 3 in [../PROJECT_SCOPE.md](../PROJECT_SCOPE.md)): it never
  mutates the recurring plan.

## Open questions for later (not blocking Sprint 1)

- Slot candidates are currently generated purely around the plan's
  preferred day/time; a real staff-availability calendar (avoiding
  double-booking a staff member across students) is a natural Phase 2
  upgrade once there's real usage data.
- Notification channel for the link (email vs SMS/WhatsApp) depends on the
  notification system built in Sprint 9 — this feature can't fully ship
  until that exists, but the data model and endpoints can be built earlier
  alongside Follow-Up Plans/Instances (Sprints 3–4).
