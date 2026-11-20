package com.school.meetings.provider;

import com.school.meetings.google.GoogleOAuthService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import tools.jackson.databind.JsonNode;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Google Calendar API v3 as a connected Google account (the shared center
 * account or a staff member's own — a free Gmail works): creates the event
 * on that account's primary calendar with a Google Meet conference attached,
 * and keeps it in sync.
 *
 * <p>Google's own invitation emails are suppressed (sendUpdates=none): the
 * app emails parents itself, the day before (see MeetingService).
 */
@Component
public class GoogleMeetProvider implements MeetingProvider {

    private static final String EVENTS_URL = "https://www.googleapis.com/calendar/v3/calendars/primary/events";

    private final GoogleOAuthService oauth;
    private final RestClient http = RestClient.create();

    public GoogleMeetProvider(GoogleOAuthService oauth) {
        this.oauth = oauth;
    }

    @Override
    public CreatedMeeting create(UUID accountId, MeetingRequest request) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("summary", request.title());
        body.put("description", request.description());
        body.put("start", time(request.start(), request.zone()));
        body.put("end", time(request.end(), request.zone()));
        body.put("attendees", request.attendeeEmails().stream().map(e -> Map.of("email", e)).toList());
        body.put("guestsCanSeeOtherGuests", false);
        body.put("conferenceData", Map.of("createRequest", Map.of(
                "requestId", request.requestId(),
                "conferenceSolutionKey", Map.of("type", "hangoutsMeet"))));

        JsonNode event = call(() -> http.post()
                .uri(EVENTS_URL + "?conferenceDataVersion=1&sendUpdates=none")
                .header(HttpHeaders.AUTHORIZATION, bearer(accountId))
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(JsonNode.class));
        return toMeeting(event);
    }

    @Override
    public CreatedMeeting fetch(UUID accountId, String eventId) {
        return toMeeting(call(() -> http.get()
                .uri(EVENTS_URL + "/{id}", eventId)
                .header(HttpHeaders.AUTHORIZATION, bearer(accountId))
                .retrieve()
                .body(JsonNode.class)));
    }

    @Override
    public void reschedule(UUID accountId, String eventId, LocalDateTime start, LocalDateTime end, ZoneId zone) {
        call(() -> http.patch()
                .uri(EVENTS_URL + "/{id}?sendUpdates=none", eventId)
                .header(HttpHeaders.AUTHORIZATION, bearer(accountId))
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("start", time(start, zone), "end", time(end, zone)))
                .retrieve()
                .body(JsonNode.class));
    }

    @Override
    public void delete(UUID accountId, String eventId) {
        try {
            call(() -> http.delete()
                    .uri(EVENTS_URL + "/{id}?sendUpdates=none", eventId)
                    .header(HttpHeaders.AUTHORIZATION, bearer(accountId))
                    .retrieve()
                    .toBodilessEntity());
        } catch (MeetingProviderException e) {
            // Already gone (404/410) is fine — the goal state is "no event".
            if (!e.getMessage().contains("HTTP 404") && !e.getMessage().contains("HTTP 410")) {
                throw e;
            }
        }
    }

    private String bearer(UUID accountId) {
        try {
            return "Bearer " + oauth.accessToken(accountId);
        } catch (IllegalStateException e) {
            throw new MeetingProviderException(e.getMessage(), e);
        }
    }

    /** Always includes seconds: LocalDateTime.toString() drops ":00", which Google rejects (HTTP 400). */
    static final DateTimeFormatter GOOGLE_LOCAL_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    static Map<String, String> time(LocalDateTime dateTime, ZoneId zone) {
        // Local wall-clock time + IANA zone: Google applies the zone (and DST) itself.
        return Map.of("dateTime", GOOGLE_LOCAL_TIME.format(dateTime), "timeZone", zone.getId());
    }

    private static CreatedMeeting toMeeting(JsonNode event) {
        String link = event.path("hangoutLink").asText(null);
        if (link == null) {
            for (JsonNode entry : event.path("conferenceData").path("entryPoints")) {
                if ("video".equals(entry.path("entryPointType").asText())) {
                    link = entry.path("uri").asText(null);
                }
            }
        }
        return new CreatedMeeting(event.path("id").asText(), link);
    }

    private static <T> T call(Supplier<T> request) {
        try {
            return request.get();
        } catch (RestClientResponseException e) {
            throw new MeetingProviderException("Google Calendar HTTP " + e.getStatusCode().value() + ": "
                    + truncate(e.getResponseBodyAsString()), e);
        } catch (MeetingProviderException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new MeetingProviderException("Google Calendar call failed: " + e.getMessage(), e);
        }
    }

    private static String truncate(String s) {
        return s == null ? "" : s.length() > 400 ? s.substring(0, 400) + "…" : s;
    }
}
