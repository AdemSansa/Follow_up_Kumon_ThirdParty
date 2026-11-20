package com.school.followup.dto;

import com.school.followup.model.Concern;
import com.school.followup.model.FollowUpStatus;
import com.school.followup.model.FrequencyType;
import com.school.followup.model.MeetingType;
import com.school.parents.model.ContactMethod;
import com.school.students.model.Subject;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * One check-in on a day's agenda (the dashboard's "Today" list), with the
 * details staff need before picking up the phone or walking into the
 * meeting: who to contact and how, who the student is, where, which
 * check-in of the plan this is, and what happened last time.
 */
public record AgendaItemResponse(
        UUID id,
        LocalDateTime scheduledStart,
        LocalDateTime scheduledEnd,
        MeetingType type,
        FollowUpStatus status,
        boolean overdue,
        /** From the follow-up plan; null for one-off follow-ups. */
        String location,
        StudentInfo student,
        /** The parent this meeting is with first (primary = true), then the student's other linked parents. */
        List<ParentContact> contacts,
        StaffInfo assignedStaff,
        PlanInfo plan,
        LastCheckIn lastCheckIn,
        long openActionItems,
        /** Google Meet state; null unless this is a VIDEO_CALL. */
        OnlineMeetingInfo meeting
) {

    /**
     * status: NONE (not created yet), PENDING, CREATED, FAILED, CANCELLED.
     * inviteSentAt: when the parent got the link for the current time, or null.
     * parentEmailMissing: no linked parent has an email, so no invite can be sent.
     */
    public record OnlineMeetingInfo(String status, String joinUrl, String lastError, java.time.Instant inviteSentAt,
                                    boolean parentEmailMissing) {
    }

    public record StudentInfo(UUID id, String firstName, String lastName, String level, Set<Subject> subjects) {
    }

    public record ParentContact(UUID id, String firstName, String lastName, String relationship, String phone,
                                String email, ContactMethod preferredContactMethod, boolean primary) {
    }

    public record StaffInfo(UUID id, String firstName, String lastName) {
    }

    /** occurrenceNumber is this check-in's position in the plan (1-based); occurrenceCount null = ongoing plan. */
    public record PlanInfo(UUID id, FrequencyType frequencyType, long occurrenceNumber, Integer occurrenceCount) {
    }

    public record LastCheckIn(LocalDate date, Integer progressRating, Integer engagementRating,
                              Integer parentSatisfactionRating, Set<Concern> concerns, String summary,
                              String recommendations) {
    }
}
