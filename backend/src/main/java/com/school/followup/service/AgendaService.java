package com.school.followup.service;

import com.school.actions.model.ActionItemStatus;
import com.school.actions.repository.ActionItemRepository;
import com.school.auth.repository.UserRepository;
import com.school.followup.dto.AgendaItemResponse;
import com.school.followup.dto.AgendaItemResponse.LastCheckIn;
import com.school.followup.dto.AgendaItemResponse.OnlineMeetingInfo;
import com.school.followup.model.MeetingType;
import com.school.meetings.repository.NotificationRepository;
import com.school.meetings.repository.OnlineMeetingRepository;
import com.school.followup.dto.AgendaItemResponse.ParentContact;
import com.school.followup.dto.AgendaItemResponse.PlanInfo;
import com.school.followup.dto.AgendaItemResponse.StaffInfo;
import com.school.followup.dto.AgendaItemResponse.StudentInfo;
import com.school.followup.model.FollowUp;
import com.school.followup.model.FollowUpPlan;
import com.school.followup.model.FollowUpStatus;
import com.school.followup.repository.FollowUpRepository;
import com.school.parents.model.Parent;
import com.school.students.model.Student;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

/** Builds a day's check-in agenda with full contact/student/plan context (dashboard "Today"). */
@Service
@Transactional(readOnly = true)
public class AgendaService {

    private static final EnumSet<FollowUpStatus> UNRESOLVED =
            EnumSet.of(FollowUpStatus.SCHEDULED, FollowUpStatus.CONFIRMED, FollowUpStatus.RESCHEDULED);
    private static final EnumSet<ActionItemStatus> OPEN_ACTIONS =
            EnumSet.of(ActionItemStatus.TODO, ActionItemStatus.IN_PROGRESS);

    private final FollowUpRepository followUpRepository;
    private final ActionItemRepository actionItemRepository;
    private final UserRepository userRepository;
    private final OnlineMeetingRepository meetingRepository;
    private final NotificationRepository notificationRepository;

    public AgendaService(FollowUpRepository followUpRepository, ActionItemRepository actionItemRepository,
                         UserRepository userRepository, OnlineMeetingRepository meetingRepository,
                         NotificationRepository notificationRepository) {
        this.followUpRepository = followUpRepository;
        this.actionItemRepository = actionItemRepository;
        this.userRepository = userRepository;
        this.meetingRepository = meetingRepository;
        this.notificationRepository = notificationRepository;
    }

    /** Every follow-up starting on {@code date} (any status), earliest first. */
    public List<AgendaItemResponse> forDay(LocalDate date) {
        LocalDateTime now = LocalDateTime.now();
        return followUpRepository
                .findByScheduledStartGreaterThanEqualAndScheduledStartLessThanOrderByScheduledStartAsc(
                        date.atStartOfDay(), date.plusDays(1).atStartOfDay())
                .stream()
                .map(f -> toItem(f, now))
                .toList();
    }

    private AgendaItemResponse toItem(FollowUp f, LocalDateTime now) {
        Student student = f.getStudent();
        FollowUpPlan plan = f.getFollowUpPlan();

        PlanInfo planInfo = plan == null ? null : new PlanInfo(plan.getId(), plan.getFrequencyType(),
                followUpRepository.countByFollowUpPlanIdAndScheduledStartLessThanEqual(plan.getId(), f.getScheduledStart()),
                plan.getOccurrenceCount());

        LastCheckIn last = followUpRepository
                .findFirstByStudentIdAndStatusAndScheduledStartBeforeOrderByScheduledStartDesc(
                        student.getId(), FollowUpStatus.COMPLETED, f.getScheduledStart())
                .map(p -> new LastCheckIn(p.getScheduledStart().toLocalDate(), p.getProgressRating(),
                        p.getEngagementRating(), p.getParentSatisfactionRating(), p.getConcerns(), p.getSummary(),
                        p.getRecommendations()))
                .orElse(null);

        StaffInfo staff = f.getAssignedStaffId() == null ? null : userRepository.findById(f.getAssignedStaffId())
                .map(u -> new StaffInfo(u.getId(), u.getFirstName(), u.getLastName()))
                .orElse(null);

        List<ParentContact> contacts = contacts(f.getParent(), student);
        OnlineMeetingInfo meeting = null;
        if (f.getType() == MeetingType.VIDEO_CALL) {
            boolean noEmail = contacts.stream().noneMatch(c -> c.email() != null && !c.email().isBlank());
            meeting = meetingRepository.findByFollowUpId(f.getId())
                    .map(m -> new OnlineMeetingInfo(m.getStatus().name(), m.getJoinUrl(), m.getLastError(),
                            notificationRepository.findInviteSentAt(f.getId(), f.getScheduledStart()), noEmail))
                    .orElse(new OnlineMeetingInfo("NONE", null, null, null, noEmail));
        }

        return new AgendaItemResponse(
                f.getId(), f.getScheduledStart(), f.getScheduledEnd(), f.getType(), f.getStatus(),
                UNRESOLVED.contains(f.getStatus()) && f.getScheduledEnd().isBefore(now),
                plan != null ? plan.getLocation() : null,
                new StudentInfo(student.getId(), student.getFirstName(), student.getLastName(), student.getLevel(),
                        student.getSubjects()),
                contacts,
                staff,
                planInfo,
                last,
                actionItemRepository.countByStudentIdAndStatusIn(student.getId(), OPEN_ACTIONS),
                meeting);
    }

    private static List<ParentContact> contacts(Parent meetingParent, Student student) {
        UUID primaryId = meetingParent != null ? meetingParent.getId() : null;
        List<Parent> parents = new ArrayList<>(student.getParents());
        if (meetingParent != null && parents.stream().noneMatch(p -> p.getId().equals(primaryId))) {
            parents.add(meetingParent);
        }
        parents.sort(Comparator.comparing((Parent p) -> !p.getId().equals(primaryId))
                .thenComparing(Parent::getLastName, Comparator.nullsLast(String::compareTo))
                .thenComparing(Parent::getFirstName, Comparator.nullsLast(String::compareTo)));
        return parents.stream()
                .map(p -> new ParentContact(p.getId(), p.getFirstName(), p.getLastName(), p.getRelationship(),
                        p.getPhone(), p.getEmail(), p.getPreferredContactMethod(), p.getId().equals(primaryId)))
                .toList();
    }
}
