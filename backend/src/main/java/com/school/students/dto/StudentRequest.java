package com.school.students.dto;

import com.school.students.model.StudentStatus;
import com.school.students.model.Subject;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;

import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

public record StudentRequest(
        @NotBlank String firstName,
        @NotBlank String lastName,
        @Past LocalDate dateOfBirth,
        String gender,
        String phone,
        @Email String email,
        String address,
        @NotNull LocalDate enrollmentDate,
        @NotNull StudentStatus status,
        String level,
        Set<Subject> subjects,
        UUID assignedTeacherId,
        Set<UUID> parentIds,
        String notes
) {
}
