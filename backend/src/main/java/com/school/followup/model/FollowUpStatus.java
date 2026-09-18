package com.school.followup.model;

/**
 * See docs/PROJECT_SCOPE.md business rules 1, 3, 6 and full-product-plan.md
 * §11. COMPLETED is set by the check-in flow (Sprint 6, not built yet) —
 * included now so the schema doesn't need another migration for it.
 */
public enum FollowUpStatus {
    SCHEDULED,
    CONFIRMED,
    COMPLETED,
    MISSED,
    CANCELLED,
    RESCHEDULED
}
