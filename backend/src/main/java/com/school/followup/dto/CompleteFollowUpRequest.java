package com.school.followup.dto;

import com.school.actions.model.ActionPriority;
import com.school.followup.model.Concern;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Payload for POST /api/follow-ups/{id}/complete — see docs/API.md. */
public record CompleteFollowUpRequest(
        @NotNull @Min(1) @Max(5) Integer progressRating,
        @NotNull @Min(1) @Max(5) Integer engagementRating,
        @NotNull @Min(1) @Max(5) Integer parentSatisfactionRating,
        Set<Concern> concerns,
        String summary,
        String recommendations,
        @Valid List<ActionItemInput> actionItems
) {

    public record ActionItemInput(
            @NotBlank String title,
            String description,
            LocalDate dueDate,
            ActionPriority priority,
            UUID assignedTo
    ) {
    }
}
