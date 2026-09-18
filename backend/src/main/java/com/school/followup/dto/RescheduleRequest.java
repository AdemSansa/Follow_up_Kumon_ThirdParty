package com.school.followup.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record RescheduleRequest(
        @NotNull LocalDateTime scheduledStart,
        @NotNull LocalDateTime scheduledEnd
) {
}
