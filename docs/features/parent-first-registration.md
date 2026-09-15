# Feature: Parent-First Registration

Deviation from the onboarding workflow sketched in
[../full-product-plan.md](../full-product-plan.md) §54 (which lists student
info first, parent info second). Changed after real usage feedback during
Sprint 2 frontend work.

## Why

Follow-up check-ins are conducted **with the parent**, not the student.
The parent is therefore the natural entry point for registration, not an
afterthought linked in from the student's form. A parent may also have
more than one child at the center, so "register the parent once, then add
each child under them" matches how staff actually think about the work.

## Flow

```text
Parents list
        │
"Register a parent" → create Parent
        │
Parent's page (edit view) shows a "Children" section
        │
"Add child" → /students/new?parentId={parentId}
        │
Student form opens with that parent pre-selected in Parents
        │
Save → returns to the parent's page (not the student's), showing the
       new child in the Children list
```

The student form still allows selecting/changing parents freely (a
student can have more than one parent/guardian, and staff can still reach
student creation directly from `/students/new` without a parent context)
— this flow is the *guided* path, not the only path.

## Implementation

- Backend: `GET /api/students?parentId={id}` — added filter
  (`StudentSpecifications.hasParent`, wired through
  `StudentService.list`/`StudentController.list`) purely to support
  listing one parent's children. See [../API.md](../API.md).
- Frontend:
  - `ParentForm` (edit mode only) fetches and lists the parent's children
    via `StudentService.list({ parentId })`, with an "Add child" action
    linking to `/students/new?parentId={id}`.
  - `StudentForm` reads the `parentId` query param on create: pre-selects
    that parent in the Parents field, and — only for that entry path —
    navigates back to `/parents/{parentId}` after saving (and on Cancel)
    instead of the new student's own page.
  - Dashboard and the sidebar nav now lead with Parents, not Students.

## Not changed

- Data model: unaffected. `Student.parents` was already a many-to-many
  relationship; this is purely a UI/API-filter change, not a schema
  change.
- `POST /api/students` / `PUT /api/students/{id}` payload shape: unchanged
  (`parentIds` on write).
