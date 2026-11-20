package com.school.followup.model;

/**
 * See docs/PROJECT_SCOPE.md business rules 1, 3, 6 and full-product-plan.md
 * §11. COMPLETED is set by the check-in flow
 * ({@code POST /api/follow-ups/{id}/complete}, Sprint 6).
 */
public enum FollowUpStatus {
    SCHEDULED,
    CONFIRMED,
    COMPLETED,
    MISSED,
    CANCELLED,
    RESCHEDULED
}
