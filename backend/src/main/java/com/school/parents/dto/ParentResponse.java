package com.school.parents.dto;

import com.school.parents.model.ContactMethod;
import com.school.parents.model.Parent;

import java.time.Instant;
import java.util.UUID;

public record ParentResponse(
        UUID id,
        String firstName,
        String lastName,
        String phone,
        String email,
        String relationship,
        ContactMethod preferredContactMethod,
        String notes,
        Instant createdAt,
        Instant updatedAt
) {

    public static ParentResponse from(Parent parent) {
        return new ParentResponse(
                parent.getId(),
                parent.getFirstName(),
                parent.getLastName(),
                parent.getPhone(),
                parent.getEmail(),
                parent.getRelationship(),
                parent.getPreferredContactMethod(),
                parent.getNotes(),
                parent.getCreatedAt(),
                parent.getUpdatedAt());
    }
}
