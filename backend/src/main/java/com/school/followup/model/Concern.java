package com.school.followup.model;

/**
 * Check-in concern checkboxes (full-product-plan.md §12). "None" is
 * represented by an empty set, not an enum value.
 */
public enum Concern {
    ATTENDANCE,
    HOMEWORK,
    MOTIVATION,
    PROGRESS,
    BEHAVIOR
}
