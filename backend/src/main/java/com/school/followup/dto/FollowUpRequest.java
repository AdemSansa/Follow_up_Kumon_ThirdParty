package com.school.followup.dto;

import com.school.followup.model.MeetingType;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.UUID;

/** Manual/ad-hoc follow-up creation — followUpPlanId is optional (not every follow-up comes from a recurring plan). */
public record FollowUpRequest(
        @NotNull UUID studentId,
        UUID followUpPlanId,
        UUID parentId,
        UUID assignedStaffId,
        @NotNull MeetingType type,
        @NotNull LocalDateTime scheduledStart,
        @NotNull LocalDateTime scheduledEnd
) {
}
