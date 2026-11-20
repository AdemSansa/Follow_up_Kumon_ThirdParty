package com.school.meetings.provider;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class GoogleMeetProviderTest {

    @Test
    void eventTimesAlwaysIncludeSecondsBecauseGoogleRejectsHHmm() {
        // LocalDateTime.toString() gives "2026-11-18T23:00" for a time on the hour — Google answers HTTP 400.
        Map<String, String> onTheHour = GoogleMeetProvider.time(LocalDateTime.of(2026, 11, 18, 23, 0), ZoneId.of("Africa/Tunis"));
        assertThat(onTheHour).containsEntry("dateTime", "2026-11-18T23:00:00").containsEntry("timeZone", "Africa/Tunis");

        Map<String, String> withNanos = GoogleMeetProvider.time(LocalDateTime.of(2026, 1, 5, 9, 30, 15, 123_000_000),
                ZoneId.of("Europe/Paris"));
        assertThat(withNanos).containsEntry("dateTime", "2026-01-05T09:30:15");
    }
}
