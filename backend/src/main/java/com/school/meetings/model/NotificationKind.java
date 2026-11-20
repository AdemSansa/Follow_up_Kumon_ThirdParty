package com.school.meetings.model;

public enum NotificationKind {
    /** The day-before email with the Meet link. */
    MEETING_INVITE,
    /** Sent when an online check-in whose invite already went out is moved. */
    MEETING_RESCHEDULED,
    /** Sent when an online check-in whose invite already went out is cancelled. */
    MEETING_CANCELLED
}
