package com.school.followup.dto;

import com.school.followup.model.FollowUpPlan;
import com.school.followup.model.FrequencyType;
import com.school.followup.model.IntervalUnit;
import com.school.followup.model.MeetingType;
import com.school.followup.model.ReminderOffset;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Set;
import java.util.UUID;

public record FollowUpPlanResponse(
        UUID id,
        PersonSummary student,
        PersonSummary parent,
        UUID assignedStaffId,
        FrequencyType frequencyType,
        Integer customIntervalValue,
        IntervalUnit customIntervalUnit,
        LocalDate startDate,
        DayOfWeek preferredDay,
        LocalTime preferredTime,
        int meetingDurationMinutes,
        String location,
        MeetingType meetingType,
        boolean active,
        LocalDate nextFollowUpDate,
        Set<ReminderOffset> reminderOffsets,
        Instant createdAt,
        Instant updatedAt
) {

    public static FollowUpPlanResponse from(FollowUpPlan plan) {
        return new FollowUpPlanResponse(
                plan.getId(),
                new PersonSummary(plan.getStudent().getId(), plan.getStudent().getFirstName(),
                        plan.getStudent().getLastName()),
                plan.getParent() == null ? null
                        : new PersonSummary(plan.getParent().getId(), plan.getParent().getFirstName(),
                        plan.getParent().getLastName()),
                plan.getAssignedStaffId(),
                plan.getFrequencyType(),
                plan.getCustomIntervalValue(),
                plan.getCustomIntervalUnit(),
                plan.getStartDate(),
                plan.getPreferredDay(),
                plan.getPreferredTime(),
                plan.getMeetingDurationMinutes(),
                plan.getLocation(),
                plan.getMeetingType(),
                plan.isActive(),
                plan.getNextFollowUpDate(),
                plan.getReminderOffsets(),
                plan.getCreatedAt(),
                plan.getUpdatedAt());
    }
}
