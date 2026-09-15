package com.school.parents.dto;

import com.school.parents.model.ContactMethod;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ParentRequest(
        @NotBlank String firstName,
        @NotBlank String lastName,
        String phone,
        @Email String email,
        @NotBlank String relationship,
        @NotNull ContactMethod preferredContactMethod,
        String notes
) {
}
