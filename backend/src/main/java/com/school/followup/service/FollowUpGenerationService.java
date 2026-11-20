package com.school.followup.service;

import com.school.followup.model.FollowUp;
import com.school.followup.model.FollowUpPlan;
import com.school.followup.model.FollowUpStatus;
import com.school.followup.repository.FollowUpRepository;
import com.school.meetings.repository.NotificationRepository;
import com.school.meetings.repository.OnlineMeetingRepository;
import com.school.students.model.StudentStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Turns a FollowUpPlan (the rule) into FollowUp rows (concrete scheduled
 * occurrences) — see docs/PROJECT_SCOPE.md "Core concept" and
 * docs/ARCHITECTURE.md "Automation engine". No cron job exists yet
 * (that's Sprint 7); this runs synchronously when a plan is created or
 * updated, which is enough to make plans immediately useful.
 */
@Service
public class FollowUpGenerationService {

    /** Matches the "next 3-6 months" recommendation in full-product-plan.md §19. */
    static final int GENERATION_WINDOW_MONTHS = 6;

    /** Safety cap so a misconfigured plan (e.g. daily CUSTOM) can't loop unboundedly. */
    static final int MAX_OCCURRENCES_PER_RUN = 400;

    private static final LocalTime DEFAULT_TIME = LocalTime.of(9, 0);

    private final FollowUpRepository followUpRepository;
    private final RecurrenceCalculator calculator;
    private final OnlineMeetingRepository meetingRepository;
    private final NotificationRepository notificationRepository;

    public FollowUpGenerationService(FollowUpRepository followUpRepository, RecurrenceCalculator calculator,
                                     OnlineMeetingRepository meetingRepository,
                                     NotificationRepository notificationRepository) {
        this.followUpRepository = followUpRepository;
        this.calculator = calculator;
        this.meetingRepository = meetingRepository;
        this.notificationRepository = notificationRepository;
    }

    /**
     * Generates any missing occurrences up to the rolling window. Existing
     * occurrences (any status) are never duplicated or touched — this only
     * fills gaps.
     */
    @Transactional
    public void generateUpcoming(FollowUpPlan plan) {
        if (!isEligible(plan)) {
            updateNextFollowUpDate(plan);
            return;
        }

        // A plan with a fixed number of check-ins is generated in full right
        // away (it's bounded), so every one shows on the calendar. An
        // open-ended plan only fills the rolling window.
        Integer limit = plan.getOccurrenceCount();
        LocalDate windowEnd = limit == null ? LocalDate.now().plusMonths(GENERATION_WINDOW_MONTHS) : null;
        LocalTime time = plan.getPreferredTime() != null ? plan.getPreferredTime() : DEFAULT_TIME;

        List<FollowUp> existing = followUpRepository.findByFollowUpPlanIdOrderByScheduledStartAsc(plan.getId());
        Set<LocalDate> existingDates = existing.stream()
                .map(f -> f.getScheduledStart().toLocalDate())
                .collect(Collectors.toSet());
        // Every occurrence the plan already has (history included) counts
        // toward the limit, so editing a plan never pushes it past N.
        int total = existing.size();

        for (int index = 0; index < MAX_OCCURRENCES_PER_RUN; index++) {
            if (limit != null && (index >= limit || total >= limit)) {
                break;
            }
            LocalDate date = calculator.occurrenceDate(plan, index);
            if (windowEnd != null && date.isAfter(windowEnd)) {
                break;
            }
            if (!existingDates.contains(date)) {
                LocalDateTime start = LocalDateTime.of(date, time);
                LocalDateTime end = start.plusMinutes(plan.getMeetingDurationMinutes());
                followUpRepository.save(new FollowUp(plan.getStudent(), plan, plan.getParent(),
                        plan.getAssignedStaffId(), plan.getMeetingType(), start, end));
                total++;
            }
        }

        updateNextFollowUpDate(plan);
    }

    /**
     * Removes not-yet-happened SCHEDULED occurrences and regenerates them
     * from the (possibly changed) rule. Never touches CONFIRMED, COMPLETED,
     * MISSED, CANCELLED, or RESCHEDULED rows — those are history or an
     * explicit staff decision, per business rules 1-3 in
     * docs/PROJECT_SCOPE.md.
     */
    @Transactional
    public void regenerateFuture(FollowUpPlan plan) {
        List<FollowUp> future = followUpRepository.findByFollowUpPlanIdAndStatusAndScheduledStartGreaterThanEqual(
                plan.getId(), FollowUpStatus.SCHEDULED, LocalDateTime.now());
        // Keep occurrences that already have a Google Meet event or a parent
        // email on record: they're commitments made to a family, so a plan
        // edit must not silently drop them (reschedule/cancel them instead).
        List<UUID> ids = future.stream().map(FollowUp::getId).toList();
        Set<UUID> committed = new HashSet<>(notificationRepository.findFollowUpIdsWithNotifications(ids));
        meetingRepository.findByFollowUpIdIn(ids).forEach(m -> committed.add(m.getFollowUp().getId()));
        followUpRepository.deleteAll(future.stream().filter(f -> !committed.contains(f.getId())).toList());
        generateUpcoming(plan);
    }

    private boolean isEligible(FollowUpPlan plan) {
        return plan.isActive() && plan.getStudent().getStatus() == StudentStatus.ACTIVE;
    }

    private void updateNextFollowUpDate(FollowUpPlan plan) {
        LocalDateTime now = LocalDateTime.now();
        plan.setNextFollowUpDate(followUpRepository.findByFollowUpPlanIdOrderByScheduledStartAsc(plan.getId())
                .stream()
                .filter(f -> f.getStatus() == FollowUpStatus.SCHEDULED || f.getStatus() == FollowUpStatus.CONFIRMED
                        || f.getStatus() == FollowUpStatus.RESCHEDULED)
                .filter(f -> !f.getScheduledStart().isBefore(now))
                .map(f -> f.getScheduledStart().toLocalDate())
                .findFirst()
                .orElse(null));
    }
}
