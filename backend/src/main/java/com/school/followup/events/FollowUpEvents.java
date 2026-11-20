package com.school.followup.events;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Published by FollowUpService inside its transaction; listeners that talk
 * to outside systems (Google Calendar, email) react AFTER_COMMIT so a
 * rolled-back change never leaks out.
 */
public final class FollowUpEvents {

    private FollowUpEvents() {
    }

    public record Rescheduled(UUID followUpId, LocalDateTime previousStart) {
    }

    public record Cancelled(UUID followUpId) {
    }
}
