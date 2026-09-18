# Frontend

Angular 22 + Angular Material, standalone components/signals. See
[../docs/ARCHITECTURE.md](../docs/ARCHITECTURE.md) for the intended module
layout and [../docs/features/parent-first-registration.md](../docs/features/parent-first-registration.md)
for the registration workflow this UI implements.

## Run locally

The backend (and its dev database) must be running first — see
[../backend/README.md](../backend/README.md). Then:

```bash
npm install
ng serve
```

Open `http://localhost:4200`. The API base URL is hardcoded in
`src/app/core/config.ts` (`http://localhost:8080/api`) — there's no
build-time environment config yet since there's only one target.

## Structure

```text
src/app/
├── core/       auth service/guard/interceptor, HTTP models, API config
├── layout/     shell (sidebar + navbar)
├── shared/     Logo brand mark
└── features/   auth (login), dashboard, students, parents, followups
```

`features/followups` covers both Follow-Up Plans (the recurrence rule —
create/edit form) and the generated FollowUp occurrences (reschedule
form). Both are surfaced from the student page, not as their own
top-level nav item yet — a plan's "New follow-up plan" / a student's
"Upcoming Follow-Ups" list is the only way to reach them.

Auth is stateless JWT: `AuthService` stores the token in `localStorage`
and exposes it via signals; `authInterceptor` attaches it to every
request and logs out on a 401.

## Build / test

```bash
ng build   # dist/frontend
ng test    # Vitest
```
