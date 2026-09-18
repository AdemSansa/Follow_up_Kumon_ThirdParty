package com.school.followup.service;

import com.school.followup.model.FollowUp;
import com.school.followup.model.FollowUpPlan;
import com.school.followup.model.FollowUpStatus;
import com.school.followup.repository.FollowUpRepository;
import com.school.students.model.StudentStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;
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

    public FollowUpGenerationService(FollowUpRepository followUpRepository, RecurrenceCalculator calculator) {
        this.followUpRepository = followUpRepository;
        this.calculator = calculator;
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

        LocalDate windowEnd = LocalDate.now().plusMonths(GENERATION_WINDOW_MONTHS);
        LocalTime time = plan.getPreferredTime() != null ? plan.getPreferredTime() : DEFAULT_TIME;

        Set<LocalDate> existingDates = followUpRepository.findByFollowUpPlanIdOrderByScheduledStartAsc(plan.getId())
                .stream()
                .map(f -> f.getScheduledStart().toLocalDate())
                .collect(Collectors.toSet());

        for (int index = 0; index < MAX_OCCURRENCES_PER_RUN; index++) {
            LocalDate date = calculator.occurrenceDate(plan, index);
            if (date.isAfter(windowEnd)) {
                break;
            }
            if (!existingDates.contains(date)) {
                LocalDateTime start = LocalDateTime.of(date, time);
                LocalDateTime end = start.plusMinutes(plan.getMeetingDurationMinutes());
                followUpRepository.save(new FollowUp(plan.getStudent(), plan, plan.getParent(),
                        plan.getAssignedStaffId(), plan.getMeetingType(), start, end));
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
        followUpRepository.deleteAll(future);
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
