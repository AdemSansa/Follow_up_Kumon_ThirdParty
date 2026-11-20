package com.school.meetings.model;

public enum MeetingStatus {
    /** Not created yet. */
    PENDING,
    /** Event + Meet link exist in Google. */
    CREATED,
    /** Last attempt failed; the job retries (see MeetingProperties.maxAttempts). */
    FAILED,
    /** Follow-up was cancelled; the Google event was removed. */
    CANCELLED
}
