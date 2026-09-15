# Student Follow-Up & Check-In Management System
## Full Product Scope, Architecture & Implementation Roadmap

> **Purpose:** Build a private-school management application for a Kumon-style accelerated learning center focused on **Math and English**, where selected students receive recurring follow-up/check-ins with their parents. The system automates scheduling, reminders, follow-up forms, notes, statistics, and calendar management.

---

# 1. Product Vision

The application turns manual student follow-up into an automated workflow.

Instead of remembering:
- Which students need follow-up?
- When is the next parent meeting?
- Which parents were contacted?
- What was discussed?
- What actions were promised?
- How is each student progressing?
- Which follow-ups are overdue?

the application continuously manages these tasks.

## Core idea

Each student can have a **Follow-Up Plan**:

```text
Student
   ↓
Follow-Up Plan
   ↓
Frequency
   ├── Weekly
   ├── Monthly
   ├── Every 3 months
   ├── Every 6 months
   └── Custom
   ↓
Automatically generated check-ins
   ↓
Calendar event
   ↓
Reminder
   ↓
Parent meeting
   ↓
Check-in form
   ↓
Notes + observations
   ↓
Next actions
   ↓
Next check-in automatically scheduled
```

---

# 2. Main Objectives

## Primary objectives

1. Centralize student information.
2. Identify students requiring follow-up.
3. Configure a different follow-up frequency per student.
4. Automatically generate future follow-ups.
5. Automatically schedule meetings.
6. Integrate with a calendar.
7. Send reminders.
8. Make the actual check-in extremely fast.
9. Store structured notes and observations.
10. Track follow-up history.
11. Provide live statistics.
12. Detect overdue and missed follow-ups.
13. Reduce administrative work.
14. Give management a clear view of student engagement and progress.

---

# 3. Important Design Principle

The application should separate:

### Student data

Long-term information about the student.

### Follow-up plan

Rules defining how frequently that student must be checked.

### Follow-up instance

One concrete check-in/meeting.

Example:

```text
Student:
Ahmed Ben Ali

Follow-up plan:
Frequency = Every Month

Generated follow-ups:
September 20
October 20
November 20
December 20
...
```

The recurring plan is the **rule**.

The generated check-ins are the **actual events**.

This distinction is critical for automation.

---

# 4. User Roles

## 4.1 Administrator / Director

Full access.

Can:
- Manage students
- Manage parents
- Manage staff
- Configure follow-up plans
- View all statistics
- Manage calendar
- View all notes
- Configure reminders
- Configure system settings

## 4.2 Teacher / Follow-Up Staff

Can:
- View assigned students
- View upcoming check-ins
- Conduct check-ins
- Add notes
- Update student observations
- Schedule/reschedule meetings
- Complete follow-up forms

Permissions should be configurable later.

---

# 5. Application Modules

```text
Dashboard
│
├── Students
│
├── Parents
│
├── Follow-Up
│
├── Calendar
│
├── Check-In
│
├── Notes
│
├── Statistics
│
├── Notifications
│
└── Settings
```

---

# 6. Dashboard

The dashboard is the command center.

## Top-level KPIs

Display live:

```text
Total Students
Students Under Follow-Up
Upcoming Check-Ins
Today's Meetings
Overdue Check-Ins
Completed This Month
Missed / Cancelled
Students Needing Attention
```

## Example dashboard

```text
------------------------------------------------
 STUDENT FOLLOW-UP DASHBOARD
------------------------------------------------

Students              247
Active Follow-Ups      64
Upcoming This Week     18
Overdue                 4

Today's Meetings        3
Completed This Month   42
Completion Rate       91%

------------------------------------------------
 ATTENTION REQUIRED

🔴 4 overdue follow-ups

🟠 7 students with negative progress

🟡 5 meetings without confirmation

------------------------------------------------
 UPCOMING

Today
14:00 - Ahmed Ben Ali - Parent Follow-Up

Tomorrow
10:30 - Sara Trabelsi - Monthly Check-In
```

---

# 7. Student Management

Each student has a profile.

## Student entity

```text
Student
- id
- firstName
- lastName
- dateOfBirth
- gender
- phone
- email
- address
- enrollmentDate
- status
- level
- subjects
- assignedTeacher
- parentIds
- notes
- createdAt
- updatedAt
```

## Student statuses

```text
ACTIVE
PAUSED
INACTIVE
COMPLETED
WITHDRAWN
```

---

# 8. Parent Management

A student can have one or multiple parents/guardians.

```text
Parent
- id
- firstName
- lastName
- phone
- email
- relationship
- preferredContactMethod
- notes
```

Possible contact methods:

```text
PHONE
EMAIL
SMS
WHATSAPP
```

---

# 9. Follow-Up Plan

This is one of the most important features.

## Follow-Up Plan

```text
FollowUpPlan
- id
- studentId
- frequencyType
- interval
- startDate
- preferredDay
- preferredTime
- meetingDuration
- assignedStaffId
- parentId
- location
- meetingType
- active
- nextFollowUpDate
- reminderSettings
- createdAt
- updatedAt
```

## Frequency options

The UI should support:

```text
Every Week
Every 2 Weeks
Every Month
Every 2 Months
Every 3 Months
Every 6 Months
Every Year
Custom
```

For custom frequency:

```text
Every [X] days/weeks/months
```

---

# 10. Follow-Up Types

Not every follow-up must be a meeting.

Support:

```text
PARENT_MEETING
PHONE_CALL
VIDEO_CALL
IN_PERSON
EMAIL_CHECK_IN
INTERNAL_REVIEW
```

For example:

```text
Monthly:
Parent meeting

Weekly:
Internal teacher review

Every 6 months:
Formal parent evaluation
```

---

# 11. Follow-Up Instance

Every scheduled occurrence becomes a separate record.

```text
FollowUp
- id
- studentId
- followUpPlanId
- parentId
- assignedStaffId

- type
- scheduledStart
- scheduledEnd

- status

- completedAt
- completedBy

- summary
- notes

- progressRating
- engagementRating
- parentSatisfactionRating

- concerns
- recommendations
- actionItems

- nextFollowUpDate

- calendarEventId

- createdAt
- updatedAt
```

## Status

```text
SCHEDULED
CONFIRMED
COMPLETED
MISSED
CANCELLED
RESCHEDULED
```

---

# 12. The Fast Check-In Experience

This is the most important UX.

The staff member should NOT have to fill out a huge form.

Target:

> A normal check-in should take 30–90 seconds to record.

## Check-in screen

```text
Ahmed Ben Ali
Monthly Parent Follow-Up

Progress
[ ⭐⭐⭐⭐⭐ ]

Parent Satisfaction
[ ⭐⭐⭐⭐⭐ ]

Student Engagement
[ ⭐⭐⭐⭐⭐ ]

Concerns
☐ Attendance
☐ Homework
☐ Motivation
☐ Progress
☐ Behavior
☐ None

Quick note
[____________________________]

Recommendation
[____________________________]

Action required?
○ No
○ Yes

[ COMPLETE CHECK-IN ]
```

---

# 13. Smart Check-In

The system can progressively make the check-in smarter.

Example:

Previous check-in:

```text
Progress: 4/5
Concern: Homework
Action: Improve home practice
```

Next check-in:

```text
Previous concern:
Homework

Did the situation improve?

○ Improved
○ Same
○ Worse
```

This makes recurring follow-ups contextual instead of repetitive.

---

# 14. Notes System

Notes should support:

## Quick notes

Simple text.

## Structured observations

```text
Academic Progress
Attendance
Homework
Behavior
Motivation
Parent Feedback
Recommendations
```

## Important notes

Allow staff to mark:

```text
Normal
Important
Critical
```

Critical notes should appear on the dashboard.

---

# 15. Action Items

A check-in can create tasks.

Example:

```text
Action:
Contact parent

Assigned to:
Teacher X

Due:
September 20

Status:
Pending
```

Entity:

```text
ActionItem
- id
- followUpId
- studentId
- assignedTo
- title
- description
- dueDate
- priority
- status
- completedAt
```

Statuses:

```text
TODO
IN_PROGRESS
DONE
CANCELLED
```

---

# 16. Calendar

The application should have an integrated calendar.

## Views

```text
Day
Week
Month
Agenda
```

## Calendar events

Each meeting should contain:

```text
Student
Parent
Staff member
Date
Start time
End time
Meeting type
Status
Notes
```

## Color/category concepts

Use visual categories such as:

```text
Upcoming
Completed
Overdue
Cancelled
High Priority
```

---

# 17. External Calendar Integration

Build the internal calendar first.

Then support external synchronization.

Potential integrations:

```text
Google Calendar
Microsoft Outlook Calendar
Apple Calendar / iCal
```

## Recommended architecture

Do NOT make the external calendar the source of truth.

Use:

```text
Your Database
      ↓
Calendar Service
      ↓
Google / Outlook
```

The database remains authoritative.

Store:

```text
externalProvider
externalEventId
externalCalendarId
syncStatus
lastSyncedAt
```

---

# 18. Calendar Automation

When a follow-up is created:

```text
Follow-Up Plan
      ↓
Generate Follow-Up
      ↓
Create Calendar Event
      ↓
Send Confirmation
      ↓
Schedule Reminder
```

When rescheduled:

```text
Follow-Up
      ↓
Update database
      ↓
Update calendar event
      ↓
Update reminders
```

---

# 19. Automated Recurring Follow-Ups

Example:

```text
Student: Ahmed
Frequency: Monthly
Start: September 20
Time: 17:00
Duration: 30 minutes
```

System generates:

```text
September 20
October 20
November 20
December 20
January 20
...
```

Do not necessarily create years of events at once.

Recommended approach:

> Generate a rolling window of future occurrences, such as the next 3–6 months.

A scheduled backend job can generate additional occurrences automatically.

---

# 20. Automation Engine

Create a dedicated automation module.

```text
Automation Engine
│
├── Follow-Up Generator
├── Reminder Scheduler
├── Overdue Detector
├── Calendar Sync
├── Notification Dispatcher
└── Statistics Updater
```

## Example daily process

```text
Every day at 02:00

1. Find active follow-up plans.
2. Generate missing upcoming follow-ups.
3. Find tomorrow's meetings.
4. Send reminders.
5. Find overdue follow-ups.
6. Mark them as overdue.
7. Generate dashboard alerts.
8. Sync calendar changes.
```

---

# 21. Reminder System

Allow reminders per follow-up plan.

Example:

```text
7 days before
24 hours before
2 hours before
30 minutes before
```

Configurable:

```text
☑ 24h before
☑ 2h before
☐ 30 min before
```

---

# 22. Notification Channels

Design the system so notification providers can be added later.

```text
Notification Service
│
├── In-App
├── Email
├── SMS
└── WhatsApp
```

Start with:

> In-app + Email

Then add SMS/WhatsApp if required.

---

# 23. Parent Communication

Future module.

Possible actions:

```text
Send meeting confirmation
Send reminder
Send follow-up summary
Send recommendation
Send missed-meeting message
```

Example workflow:

```text
Check-in completed
      ↓
Generate summary
      ↓
Staff reviews
      ↓
Send to parent
```

The message should require human approval initially rather than automatically sending sensitive notes.

---

# 24. Student Progress

Create a simple progress model.

Example:

```text
Progress
1 = Significant concern
2 = Needs improvement
3 = Stable
4 = Good
5 = Excellent
```

Track over time.

Example:

```text
September   3
October     4
November    4
December    5
```

This enables graphs.

---

# 25. Statistics

## Management statistics

```text
Total Students
Active Students
Students Under Follow-Up
Follow-Ups Completed
Follow-Ups Pending
Overdue Follow-Ups
Missed Meetings
Completion Rate
```

## Student statistics

```text
Follow-Up Count
Completed Count
Missed Count
Progress Evolution
Attendance Evolution
Parent Satisfaction
Open Action Items
```

## Time statistics

```text
Follow-ups per week
Follow-ups per month
Completion rate
Average completion time
Overdue rate
```

---

# 26. Live Dashboard

The dashboard should update without requiring a page refresh.

Recommended technology:

```text
WebSocket
```

Events:

```text
FOLLOWUP_CREATED
FOLLOWUP_COMPLETED
FOLLOWUP_RESCHEDULED
FOLLOWUP_CANCELLED
ACTION_CREATED
ACTION_COMPLETED
STUDENT_UPDATED
```

For a simpler MVP, normal API refresh/polling is acceptable.

---

# 27. Search & Filters

Students should be searchable by:

```text
Name
Parent
Phone
Level
Subject
Teacher
Status
Follow-Up Status
```

Follow-up filters:

```text
Today
This Week
This Month
Overdue
Completed
Missed
Upcoming
```

---

# 28. Student Profile UX

Suggested structure:

```text
------------------------------------------------
Ahmed Ben Ali

Level: A
Subjects: Math + English
Status: Active
------------------------------------------------

FOLLOW-UP
Next check-in:
September 20, 17:00

Frequency:
Monthly

------------------------------------------------

PROGRESS

     📈
  Progress graph

------------------------------------------------

RECENT FOLLOW-UPS

Sep 20  ✓ Completed
Aug 20  ✓ Completed
Jul 20  ⚠ Concern

------------------------------------------------

NOTES

...

------------------------------------------------

ACTIONS

[ Schedule Follow-Up ]
[ Start Check-In ]
[ Add Note ]
```

---

# 29. Recommended Technical Architecture

For this type of system, use a web-based architecture.

```text
                    ┌──────────────────────┐
                    │      Frontend        │
                    │      Angular         │
                    └──────────┬───────────┘
                               │
                          REST / WS
                               │
                    ┌──────────▼───────────┐
                    │       Backend        │
                    │   Spring Boot API    │
                    └──────────┬───────────┘
                               │
          ┌────────────────────┼────────────────────┐
          │                    │                    │
          ▼                    ▼                    ▼
      Database             Scheduler          Notification
      MongoDB              / Jobs              Service
          │                    │                    │
          │                    │                    ├── Email
          │                    │                    ├── SMS
          │                    │                    └── WhatsApp
          │                    │
          ▼                    ▼
      Student Data       Follow-Up Automation

                               │
                               ▼
                         Calendar Service
                               │
                     ┌─────────┴─────────┐
                     ▼                   ▼
                Google Calendar      Outlook
```

---

# 30. Recommended Stack

Because this is a business management application:

## Frontend

```text
Angular
TypeScript
Angular Material
RxJS
```

## Backend

```text
Spring Boot
Java
Spring Security
Spring Data
WebSocket
```

## Database

Recommended:

```text
PostgreSQL
```

### Why PostgreSQL?

This application contains highly relational data:

```text
Student
  ↓
Parent
  ↓
Follow-Up Plan
  ↓
Follow-Up
  ↓
Notes
  ↓
Action Items
  ↓
Calendar Event
```

A relational database makes these relationships, reporting, constraints, and transactional operations easier.

MongoDB can work, but PostgreSQL is the stronger default for this specific system.

---

# 31. Infrastructure

Recommended production architecture:

```text
Frontend
   ↓
Cloud hosting / CDN

Backend
   ↓
Cloud server / container

Database
   ↓
Managed PostgreSQL

Email
   ↓
Transactional email provider

Calendar
   ↓
Google / Microsoft APIs

Monitoring
   ↓
Application logs + error tracking
```

Use Docker for the backend and local development.

---

# 32. Backend Architecture

Use a modular monolith first.

Do NOT start with microservices.

```text
backend/
└── src/main/java/com/school/
    │
    ├── auth/
    │   ├── controller/
    │   ├── service/
    │   ├── repository/
    │   └── model/
    │
    ├── students/
    │   ├── controller/
    │   ├── service/
    │   ├── repository/
    │   └── model/
    │
    ├── parents/
    │
    ├── followup/
    │   ├── controller/
    │   ├── service/
    │   ├── repository/
    │   ├── scheduler/
    │   └── model/
    │
    ├── calendar/
    │
    ├── notifications/
    │
    ├── notes/
    │
    ├── actions/
    │
    ├── statistics/
    │
    └── common/
```

---

# 33. Frontend Architecture

```text
frontend/
└── src/app/
    │
    ├── core/
    │   ├── auth/
    │   ├── guards/
    │   ├── interceptors/
    │   └── services/
    │
    ├── shared/
    │   ├── components/
    │   ├── dialogs/
    │   ├── tables/
    │   └── pipes/
    │
    ├── features/
    │   ├── dashboard/
    │   ├── students/
    │   ├── parents/
    │   ├── followups/
    │   ├── calendar/
    │   ├── checkins/
    │   ├── notes/
    │   ├── actions/
    │   ├── statistics/
    │   └── settings/
    │
    └── layout/
        ├── sidebar/
        ├── navbar/
        └── shell/
```

---

# 34. Database Schema

Core tables:

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
  ├────────────── assigned to ──────────────┐
  │                                         │
  ▼                                         ▼
students ─────── follow_up_plans ─────── follow_ups
   │                                         │
   │                                         ├── notes
   │                                         ├── actions
   │                                         └── calendar_event
   │
   └──── student_parents ─── parents
```

---

# 35. API Design

Use REST.

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

---

# 36. Check-In API

Use a dedicated endpoint for the important operation.

```http
POST /api/follow-ups/{id}/complete
```

Example payload:

```json
{
  "progressRating": 4,
  "engagementRating": 5,
  "parentSatisfactionRating": 4,
  "concerns": [
    "HOMEWORK"
  ],
  "summary": "Good progress this month.",
  "recommendations": "Continue regular practice.",
  "actionItems": [
    {
      "title": "Monitor homework",
      "dueDate": "2026-10-01"
    }
  ]
}
```

The backend should execute the complete workflow transactionally.

---

# 37. Completing a Check-In

When the user presses:

```text
COMPLETE CHECK-IN
```

Backend:

```text
1. Validate follow-up.
2. Validate permissions.
3. Save ratings.
4. Save notes.
5. Save concerns.
6. Create action items.
7. Mark follow-up COMPLETED.
8. Calculate next follow-up.
9. Create/schedule next follow-up.
10. Create calendar event.
11. Schedule reminders.
12. Update statistics.
13. Publish real-time event.
```

This is the core automation workflow.

---

# 38. Security

Because student and parent information is private, security is essential.

Implement:

```text
JWT authentication
Role-based authorization
Password hashing
HTTPS
Input validation
Database constraints
Audit logging
Rate limiting
Secure HTTP headers
```

Never expose:

```text
passwords
JWT secrets
API keys
OAuth secrets
database credentials
```

in the frontend.

---

# 39. Audit Log

Important operations should be recorded.

```text
AuditLog
- id
- userId
- action
- entityType
- entityId
- timestamp
- metadata
```

Examples:

```text
STUDENT_CREATED
STUDENT_UPDATED
FOLLOWUP_CREATED
FOLLOWUP_COMPLETED
FOLLOWUP_RESCHEDULED
NOTE_CREATED
PARENT_UPDATED
```

This is useful for accountability and debugging.

---

# 40. Automation Rules

Create an explicit rules engine rather than scattering automation logic throughout controllers.

Example:

```text
FollowUpAutomationService

generateUpcomingFollowUps()
scheduleReminder()
detectOverdueFollowUps()
createCalendarEvent()
rescheduleCalendarEvent()
createNextOccurrence()
```

Controllers should call services.

Controllers should NOT contain scheduling/business logic.

---

# 41. Overdue Logic

A scheduled follow-up becomes overdue when:

```text
scheduledEnd < currentTime
AND
status != COMPLETED
AND
status != CANCELLED
```

Then:

```text
status → MISSED / OVERDUE
```

Prefer adding an explicit `OVERDUE` status if management needs to distinguish:

```text
MISSED = appointment actually missed
OVERDUE = staff has not completed the follow-up yet
```

---

# 42. Recurrence Edge Cases

The recurrence engine must handle:

### Month-end

Example:

```text
January 31
```

What happens in February?

Define a rule:

```text
LAST_DAY_OF_MONTH
```

or

```text
SAME_DAY_WHEN_POSSIBLE
```

### Holidays

Allow future holiday/calendar rules.

### Rescheduling

Rescheduling one occurrence should NOT automatically change the entire recurring plan unless explicitly requested.

### Pausing a student

Pausing the student should pause future follow-ups.

### Deactivating a plan

Existing historical follow-ups remain.

Future pending occurrences are cancelled or archived.

---

# 43. Calendar Sync Strategy

Use an adapter pattern.

```text
CalendarService
      │
      ├── GoogleCalendarAdapter
      │
      └── OutlookCalendarAdapter
```

Interface:

```text
createEvent()
updateEvent()
deleteEvent()
getEvent()
```

This allows additional providers later.

---

# 44. Email Architecture

Create:

```text
NotificationService
```

with:

```text
sendFollowUpReminder()
sendMeetingConfirmation()
sendRescheduleNotification()
sendFollowUpSummary()
```

Use templates.

Example:

```text
Hello Mr/Ms [Parent],

This is a reminder that the follow-up meeting
for [Student] is scheduled for:

Date: [Date]
Time: [Time]

Regards,
[School]
```

---

# 45. UX Principle: One-Click Operations

Common actions should be extremely fast.

From dashboard:

```text
[✓ Complete]
[↻ Reschedule]
[📞 Contact Parent]
[📝 Add Note]
```

From student profile:

```text
[Start Check-In]
```

Avoid forcing staff to navigate through multiple pages.

---

# 46. Mobile Responsiveness

The application should work on:

```text
Desktop
Laptop
Tablet
Phone
```

Priority:

```text
Desktop → administration
Mobile → quick check-ins
```

A staff member should be able to complete a check-in from a phone.

---

# 47. MVP

Do NOT build everything immediately.

## MVP Phase 1

Build:

```text
Authentication
Students
Parents
Follow-Up Plans
Follow-Up Instances
Calendar
Check-In
Notes
Dashboard
Basic Statistics
```

Automation:

```text
Recurring follow-ups
Reminder system
Overdue detection
Automatic next appointment
```

This is enough to make the application genuinely useful.

---

# 48. Phase 2

Add:

```text
Google Calendar
Outlook Calendar
Email notifications
Parent communication
Action items
Advanced statistics
Progress graphs
Audit logs
```

---

# 49. Phase 3

Advanced automation:

```text
WhatsApp integration
SMS
AI-generated summaries
AI detection of students needing attention
AI-generated parent summaries
Predictive follow-up recommendations
Advanced reporting
```

AI should be added AFTER the core workflow works.

---

# 50. AI Features — Later

Potential AI:

## Student Risk Detection

Analyze:

```text
Progress
Attendance
Missed follow-ups
Teacher notes
Parent feedback
```

Then calculate:

```text
LOW RISK
MEDIUM RISK
HIGH RISK
```

## AI Summary

After a check-in:

```text
Raw notes
    ↓
AI
    ↓
Professional summary
    ↓
Staff approval
    ↓
Parent communication
```

Important:

> AI-generated content should be reviewed by staff before being sent to parents.

---

# 51. Recommended UI Pages

```text
/login

/dashboard

/students
/students/new
/students/:id
/students/:id/edit

/parents
/parents/:id

/follow-ups
/follow-ups/:id
/follow-ups/:id/check-in

/calendar

/notes

/actions

/statistics

/settings
/settings/users
/settings/notifications
/settings/calendar
```

---

# 52. Dashboard Navigation

Recommended sidebar:

```text
🏠 Dashboard

👨‍🎓 Students

👨‍👩‍👧 Parents

📅 Calendar

🔄 Follow-Ups

📝 Notes

✅ Actions

📊 Statistics

⚙ Settings
```

---

# 53. Follow-Up List

Example:

```text
---------------------------------------------------------
FOLLOW-UPS
---------------------------------------------------------

Student       Date        Type       Status       Action

Ahmed         Sep 20      Meeting    Upcoming     [Open]
Sara          Sep 21      Call       Upcoming     [Open]
Yassine       Sep 18      Meeting    OVERDUE      [Open]
Mariem        Sep 15      Meeting    Completed    [View]
---------------------------------------------------------
```

Filters:

```text
Status
Date
Teacher
Frequency
Student
Parent
```

---

# 54. Onboarding Workflow

When adding a student:

```text
Step 1
Student information

        ↓

Step 2
Parent information

        ↓

Step 3
Subjects / level

        ↓

Step 4
Does this student require follow-up?

YES

        ↓

Step 5
Configure frequency

Weekly / Monthly / 6 months / Custom

        ↓

Step 6
Choose staff member

        ↓

Step 7
Choose day/time

        ↓

Step 8
Reminder settings

        ↓

CREATE
```

This makes onboarding automatic.

---

# 55. Example Complete Workflow

Student:

```text
Name: Ahmed
Subject: Mathematics
```

Admin selects:

```text
Follow-up required: YES

Frequency:
Every Month

Parent:
Ahmed's mother

Meeting:
In person

Day:
Saturday

Time:
10:00

Duration:
30 minutes

Reminder:
24 hours before
```

System:

```text
Creates Follow-Up Plan
        ↓
Creates next occurrence
        ↓
Creates calendar event
        ↓
Schedules reminder
```

One month later:

```text
Reminder sent
        ↓
Parent attends
        ↓
Teacher opens Check-In
        ↓
Rates progress
        ↓
Adds note
        ↓
Clicks COMPLETE
        ↓
System creates next follow-up
        ↓
Calendar updated
        ↓
Statistics updated
```

The staff member does not manually recreate the next appointment.

---

# 56. Project Development Roadmap

## Sprint 0 — Planning

Create:

```text
README.md
PROJECT_SCOPE.md
ARCHITECTURE.md
API.md
DATABASE.md
```

Set:

```text
Git repository
Docker
Environment variables
Development database
CI/CD later
```

---

# 57. Sprint 1 — Backend Foundation

Implement:

```text
Spring Boot project
PostgreSQL
Flyway migrations
Spring Security
JWT
User entity
Roles
Global exception handling
Validation
Logging
```

Test:

```text
Login
Authentication
Authorization
```

---

# 58. Sprint 2 — Students & Parents

Implement:

```text
Student CRUD
Parent CRUD
Student-parent relationship
Search
Filters
Pagination
```

Frontend:

```text
Student list
Student form
Student profile
Parent list
Parent form
```

---

# 59. Sprint 3 — Follow-Up Plans

Implement:

```text
FollowUpPlan
Frequency
Start date
Time
Duration
Staff assignment
Parent assignment
Active/inactive
```

Frontend:

```text
Create plan
Edit plan
Pause plan
Resume plan
Delete/deactivate plan
```

---

# 60. Sprint 4 — Follow-Up Engine

Implement:

```text
FollowUp entity
Recurrence calculation
Occurrence generation
Overdue detection
Rescheduling
Cancellation
```

Write tests for:

```text
Weekly
Biweekly
Monthly
3 months
6 months
Yearly
Month-end
Leap years
Paused plans
Cancelled plans
```

---

# 61. Sprint 5 — Calendar

Build:

```text
Day view
Week view
Month view
Event details
Create event
Reschedule event
```

Connect follow-ups to calendar events.

---

# 62. Sprint 6 — Check-In

Build the fast check-in UI.

Features:

```text
Progress rating
Engagement rating
Parent satisfaction
Concerns
Quick note
Recommendation
Action items
Complete button
```

Backend completion workflow should be transactional.

---

# 63. Sprint 7 — Automation

Implement scheduled jobs:

```text
Generate future follow-ups
Reminder scheduler
Overdue detection
Calendar synchronization
Notification processing
```

Use:

```text
Spring Scheduler
```

For a more advanced setup, use a job system/queue later.

---

# 64. Sprint 8 — Dashboard & Statistics

Implement:

```text
KPIs
Upcoming meetings
Overdue list
Completion rate
Progress charts
Follow-up charts
```

---

# 65. Sprint 9 — Notifications

Implement:

```text
In-app notifications
Email
Reminder preferences
Meeting confirmation
Reschedule notification
```

---

# 66. Sprint 10 — Security & Audit

Implement:

```text
Audit logs
Permission checks
Rate limiting
Security headers
Validation
Data access controls
Backup strategy
```

---

# 67. Sprint 11 — External Calendar

Implement:

```text
Google OAuth
Google Calendar events
Sync
Update
Delete
Token management
```

Then:

```text
Outlook
```

---

# 68. Sprint 12 — Production

Prepare:

```text
Docker
Production environment variables
HTTPS
Database backup
Monitoring
Error tracking
Logging
CI/CD
Domain
```

Then deploy.

---

# 69. Testing Strategy

## Unit tests

Test:

```text
Recurrence calculations
Follow-up status
Overdue logic
Statistics
Permissions
```

## Integration tests

Test:

```text
Student → Follow-Up Plan
Follow-Up → Calendar
Check-In → Next Follow-Up
Check-In → Action Item
Reminder → Notification
```

## End-to-end tests

Scenario:

```text
Create student
→ create parent
→ create follow-up plan
→ generate appointment
→ open appointment
→ complete check-in
→ verify next appointment
→ verify dashboard
```

---

# 70. Seed Data

Create development seed data:

```text
20 students
10 parents
5 teachers
Different follow-up frequencies
Completed follow-ups
Upcoming follow-ups
Overdue follow-ups
Cancelled meetings
Different progress ratings
```

This makes the dashboard realistic during development.

---

# 71. Environment Variables

Never hardcode secrets.

Example:

```env
DATABASE_URL=
DATABASE_USERNAME=
DATABASE_PASSWORD=

JWT_SECRET=

MAIL_HOST=
MAIL_USERNAME=
MAIL_PASSWORD=

GOOGLE_CLIENT_ID=
GOOGLE_CLIENT_SECRET=

OUTLOOK_CLIENT_ID=
OUTLOOK_CLIENT_SECRET=
```

---

# 72. Git Strategy

Branches:

```text
main
develop

feature/auth
feature/students
feature/followups
feature/calendar
feature/checkin
feature/notifications
feature/statistics
```

Commit examples:

```text
feat: add student management
feat: add follow-up recurrence engine
feat: implement check-in completion
fix: handle monthly recurrence at month end
```

---

# 73. Definition of Done

A feature is not finished just because the UI works.

For each feature:

```text
☐ Database model
☐ Migration
☐ Backend entity
☐ Repository
☐ Service
☐ Controller
☐ Validation
☐ Authorization
☐ Unit tests
☐ Frontend service
☐ Frontend UI
☐ Loading state
☐ Error state
☐ Empty state
☐ Responsive design
☐ Documentation
```

---

# 74. Most Important Business Rules

Document these centrally.

```text
1. Historical follow-ups must never be deleted accidentally.

2. Deactivating a follow-up plan does not delete history.

3. Rescheduling one occurrence does not change the recurring plan.

4. Completing a follow-up automatically determines the next occurrence.

5. A paused student should not receive new automatic follow-ups.

6. Cancelled meetings must not be treated as completed.

7. Overdue follow-ups must be visible to management.

8. Only authorized staff can view/edit student information.

9. Parent-facing summaries should not automatically expose internal notes.

10. Calendar synchronization must not become the source of truth.
```

---

# 75. Final Architecture

The finished system should conceptually look like this:

```text
                         ┌─────────────────┐
                         │     USERS       │
                         └────────┬────────┘
                                  │
                         ┌────────▼────────┐
                         │    ANGULAR      │
                         │    FRONTEND     │
                         └────────┬────────┘
                                  │
                         REST + WebSocket
                                  │
                         ┌────────▼────────┐
                         │   SPRING BOOT   │
                         │     BACKEND     │
                         └────────┬────────┘
                                  │
       ┌───────────────┬──────────┼───────────┬───────────────┐
       │               │          │           │               │
       ▼               ▼          ▼           ▼               ▼
   Students       Follow-Up   Calendar    Notifications   Statistics
       │               │          │           │
       │               │          │           ├── Email
       │               │          │           ├── SMS later
       │               │          │           └── WhatsApp later
       │               │          │
       │               │          ├── Google
       │               │          └── Outlook
       │               │
       │               ├── Recurrence Engine
       │               ├── Check-In
       │               ├── Notes
       │               └── Actions
       │
       └───────────────────────┐
                               ▼
                         PostgreSQL
```

---

# 76. Recommended Build Order

Do NOT start with the calendar or AI.

Build exactly in this order:

```text
1. Project setup
        ↓
2. Authentication
        ↓
3. Students
        ↓
4. Parents
        ↓
5. Follow-Up Plans
        ↓
6. Follow-Up Instances
        ↓
7. Recurrence Engine
        ↓
8. Check-In
        ↓
9. Notes + Actions
        ↓
10. Internal Calendar
        ↓
11. Dashboard
        ↓
12. Statistics
        ↓
13. Notifications
        ↓
14. Automation
        ↓
15. Google Calendar
        ↓
16. Outlook
        ↓
17. Advanced reporting
        ↓
18. AI
```

---

# 77. First Development Milestone

The first usable version should achieve this:

```text
ADMIN
  ↓
Creates Student
  ↓
Adds Parent
  ↓
Selects:
"Monthly Follow-Up"
  ↓
System creates appointment
  ↓
Appointment appears on calendar
  ↓
Staff receives reminder
  ↓
Staff opens appointment
  ↓
Completes 60-second check-in
  ↓
Adds notes
  ↓
Clicks COMPLETE
  ↓
System automatically schedules next month
  ↓
Dashboard updates
```

If this workflow works perfectly, the core product is already valuable.

Everything else should be built around improving this workflow.

---

# 78. Product North Star

The application should answer one question instantly:

> **"Which students need my attention, what happened last time, and what do I need to do next?"**

The dashboard should make that answer obvious within seconds.

The long-term goal is:

```text
MANUAL FOLLOW-UP
       ↓
STRUCTURED FOLLOW-UP
       ↓
AUTOMATED FOLLOW-UP
       ↓
INTELLIGENT FOLLOW-UP
```

Start with automation and reliability before adding AI.
