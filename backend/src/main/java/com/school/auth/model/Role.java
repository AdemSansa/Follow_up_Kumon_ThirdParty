package com.school.auth.model;

/**
 * See docs/PROJECT_SCOPE.md §User roles. ADMIN = Administrator/Director
 * (full access), STAFF = Teacher/Follow-Up Staff (assigned students only).
 */
public enum Role {
    ADMIN,
    STAFF
}
