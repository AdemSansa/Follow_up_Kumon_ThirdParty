# Documentation Index

- [full-product-plan.md](./full-product-plan.md) — full product scope, architecture & sprint-by-sprint roadmap (source of truth)
- [PROJECT_SCOPE.md](./PROJECT_SCOPE.md) — condensed objectives, modules, MVP boundary, business rules
- [ARCHITECTURE.md](./ARCHITECTURE.md) — system architecture, module layout, automation engine
- [API.md](./API.md) — REST API reference
- [DATABASE.md](./DATABASE.md) — schema and entity relationships
- [TEST_ACCOUNTS.md](./TEST_ACCOUNTS.md) — local dev login credentials (bootstrap ADMIN/STAFF accounts)

## Features

Extensions to the core plan, added after initial planning, documented
separately so they don't get lost in the condensed docs above:

- [features/parent-slot-scheduling.md](./features/parent-slot-scheduling.md) — parent picks a check-in time from proposed slots, token-based, no parent account
- [features/parent-first-registration.md](./features/parent-first-registration.md) — register the parent first, add children from the parent's page (check-ins are with the parent, not the student)

Keep PROJECT_SCOPE/ARCHITECTURE/API/DATABASE in sync with the code as each
sprint lands; `full-product-plan.md` stays as the original reference and is
not edited.
