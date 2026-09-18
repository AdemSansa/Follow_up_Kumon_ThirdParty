package com.school.followup.dto;

import java.util.UUID;

/** Lightweight reference used inside FollowUpPlanResponse — avoids pulling a full nested student/parent payload. */
public record PersonSummary(UUID id, String firstName, String lastName) {
}
