package com.school.meetings.provider;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

/**
 * Creates/updates/deletes the calendar event that carries a video link, as
 * one connected Google account ({@code accountId} = google_accounts.id).
 * {@link GoogleMeetProvider} is the real one; tests swap in a fake.
 */
public interface MeetingProvider {

    CreatedMeeting create(UUID accountId, MeetingRequest request);

    /** Re-reads an existing event (e.g. when the Meet link was still being provisioned). */
    CreatedMeeting fetch(UUID accountId, String eventId);

    void reschedule(UUID accountId, String eventId, LocalDateTime start, LocalDateTime end, ZoneId zone);

    void delete(UUID accountId, String eventId);

    record MeetingRequest(
            String requestId,
            String title,
            String description,
            LocalDateTime start,
            LocalDateTime end,
            ZoneId zone,
            List<String> attendeeEmails
    ) {
    }

    /** joinUrl may be null if Google is still provisioning the Meet link; fetch() later. */
    record CreatedMeeting(String eventId, String joinUrl) {
    }

    class MeetingProviderException extends RuntimeException {
        public MeetingProviderException(String message) {
            super(message);
        }

        public MeetingProviderException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
