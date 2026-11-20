package com.school.followup.dto;

import com.school.followup.model.FrequencyType;
import com.school.followup.model.IntervalUnit;
import com.school.followup.model.MeetingType;
import com.school.followup.model.ReminderOffset;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Set;
import java.util.UUID;

public record FollowUpPlanRequest(
        @NotNull UUID studentId,
        UUID parentId,
        UUID assignedStaffId,
        @NotNull FrequencyType frequencyType,
        Integer customIntervalValue,
        IntervalUnit customIntervalUnit,
        @NotNull LocalDate startDate,
        DayOfWeek preferredDay,
        LocalTime preferredTime,
        @Min(5) Integer meetingDurationMinutes,
        String location,
        @NotNull MeetingType meetingType,
        Set<ReminderOffset> reminderOffsets,
        Boolean active,
        /** Total number of check-ins for this plan; null = ongoing (no end). */
        @Min(1) @Max(FollowUpPlanRequest.MAX_OCCURRENCE_COUNT) Integer occurrenceCount
) {
    public static final int MAX_OCCURRENCE_COUNT = 200;
}
