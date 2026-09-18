package com.school.followup.service;

import com.school.followup.model.FollowUpPlan;
import com.school.followup.model.IntervalUnit;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * Computes the Nth occurrence date for a plan's recurrence rule.
 *
 * <p>Deliberately computes every occurrence as an offset from the plan's
 * own {@code startDate} ({@code startDate.plus(N * interval)}), never by
 * repeatedly adding the interval to the previously computed date. The
 * latter drifts at month-end: chaining {@code plusMonths(1)} from Jan 31
 * gives Feb 28, then Mar 28 (not Mar 31) — {@link java.time.LocalDate}
 * clamps the day only when the target month doesn't have it, and that
 * clamp is expected to be independent each time, not permanent. Computing
 * every occurrence from the same fixed {@code startDate} keeps each one
 * independently clamped, which is what "same day when possible" (see
 * full-product-plan.md §42) actually requires — and gets leap-year
 * Feb 29 right the same way (each year clamps to Feb 28 independently,
 * not permanently, so a future leap year correctly lands back on Feb 29).
 */
@Component
public class RecurrenceCalculator {

    public LocalDate occurrenceDate(FollowUpPlan plan, int occurrenceIndex) {
        if (occurrenceIndex < 0) {
            throw new IllegalArgumentException("occurrenceIndex must be >= 0");
        }
        LocalDate start = plan.getStartDate();
        return switch (plan.getFrequencyType()) {
            case WEEKLY -> start.plusWeeks(occurrenceIndex);
            case EVERY_2_WEEKS -> start.plusWeeks(occurrenceIndex * 2L);
            case MONTHLY -> start.plusMonths(occurrenceIndex);
            case EVERY_2_MONTHS -> start.plusMonths(occurrenceIndex * 2L);
            case EVERY_3_MONTHS -> start.plusMonths(occurrenceIndex * 3L);
            case EVERY_6_MONTHS -> start.plusMonths(occurrenceIndex * 6L);
            case YEARLY -> start.plusYears(occurrenceIndex);
            case CUSTOM -> customOccurrenceDate(plan, start, occurrenceIndex);
        };
    }

    private LocalDate customOccurrenceDate(FollowUpPlan plan, LocalDate start, int occurrenceIndex) {
        int value = plan.getCustomIntervalValue();
        IntervalUnit unit = plan.getCustomIntervalUnit();
        long amount = (long) value * occurrenceIndex;
        return switch (unit) {
            case DAYS -> start.plusDays(amount);
            case WEEKS -> start.plusWeeks(amount);
            case MONTHS -> start.plusMonths(amount);
        };
    }
}
