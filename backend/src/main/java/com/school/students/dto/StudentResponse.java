package com.school.students.dto;

import com.school.parents.dto.ParentResponse;
import com.school.students.model.Student;
import com.school.students.model.StudentStatus;
import com.school.students.model.Subject;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public record StudentResponse(
        UUID id,
        String firstName,
        String lastName,
        LocalDate dateOfBirth,
        String gender,
        String phone,
        String email,
        String address,
        LocalDate enrollmentDate,
        StudentStatus status,
        String level,
        Set<Subject> subjects,
        UUID assignedTeacherId,
        Set<ParentResponse> parents,
        String notes,
        /** When the profile photo last changed; null = no photo. Also used by the UI to refresh its cached image. */
        Instant photoUpdatedAt,
        Instant createdAt,
        Instant updatedAt
) {

    public static StudentResponse from(Student student, Instant photoUpdatedAt) {
        return new StudentResponse(
                student.getId(),
                student.getFirstName(),
                student.getLastName(),
                student.getDateOfBirth(),
                student.getGender(),
                student.getPhone(),
                student.getEmail(),
                student.getAddress(),
                student.getEnrollmentDate(),
                student.getStatus(),
                student.getLevel(),
                student.getSubjects(),
                student.getAssignedTeacherId(),
                student.getParents().stream().map(ParentResponse::from).collect(Collectors.toSet()),
                student.getNotes(),
                photoUpdatedAt,
                student.getCreatedAt(),
                student.getUpdatedAt());
    }
}
