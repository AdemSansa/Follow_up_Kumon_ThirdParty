package com.school.followup.dto;

import com.school.followup.model.FollowUp;
import com.school.followup.model.FollowUpStatus;
import com.school.followup.model.MeetingType;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

public record FollowUpResponse(
        UUID id,
        PersonSummary student,
        UUID followUpPlanId,
        PersonSummary parent,
        UUID assignedStaffId,
        MeetingType type,
        LocalDateTime scheduledStart,
        LocalDateTime scheduledEnd,
        FollowUpStatus status,
        boolean overdue,
        Instant createdAt,
        Instant updatedAt
) {

    public static FollowUpResponse from(FollowUp followUp) {
        boolean unresolved = followUp.getStatus() == FollowUpStatus.SCHEDULED
                || followUp.getStatus() == FollowUpStatus.CONFIRMED
                || followUp.getStatus() == FollowUpStatus.RESCHEDULED;
        boolean overdue = unresolved && followUp.getScheduledEnd().isBefore(LocalDateTime.now());

        return new FollowUpResponse(
                followUp.getId(),
                new PersonSummary(followUp.getStudent().getId(), followUp.getStudent().getFirstName(),
                        followUp.getStudent().getLastName()),
                followUp.getFollowUpPlan() != null ? followUp.getFollowUpPlan().getId() : null,
                followUp.getParent() == null ? null
                        : new PersonSummary(followUp.getParent().getId(), followUp.getParent().getFirstName(),
                        followUp.getParent().getLastName()),
                followUp.getAssignedStaffId(),
                followUp.getType(),
                followUp.getScheduledStart(),
                followUp.getScheduledEnd(),
                followUp.getStatus(),
                overdue,
                followUp.getCreatedAt(),
                followUp.getUpdatedAt());
    }
}
