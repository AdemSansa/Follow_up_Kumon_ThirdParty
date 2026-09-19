package com.school.actions.dto;

import com.school.actions.model.ActionItem;
import com.school.actions.model.ActionItemStatus;
import com.school.actions.model.ActionPriority;
import com.school.followup.dto.PersonSummary;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record ActionItemResponse(
        UUID id,
        UUID followUpId,
        PersonSummary student,
        UUID assignedTo,
        String title,
        String description,
        LocalDate dueDate,
        ActionPriority priority,
        ActionItemStatus status,
        Instant completedAt,
        Instant createdAt
) {

    public static ActionItemResponse from(ActionItem item) {
        return new ActionItemResponse(
                item.getId(),
                item.getFollowUp().getId(),
                new PersonSummary(item.getStudent().getId(), item.getStudent().getFirstName(),
                        item.getStudent().getLastName()),
                item.getAssignedTo(),
                item.getTitle(),
                item.getDescription(),
                item.getDueDate(),
                item.getPriority(),
                item.getStatus(),
                item.getCompletedAt(),
                item.getCreatedAt());
    }
}
