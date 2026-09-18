package com.school.followup;

import com.school.followup.model.FollowUpPlan;
import com.school.followup.model.FrequencyType;
import com.school.followup.model.IntervalUnit;
import com.school.followup.model.MeetingType;
import com.school.followup.service.RecurrenceCalculator;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pure logic, no Spring context — exercises every frequency plus the
 * month-end and leap-year edge cases called out in
 * full-product-plan.md §42 and Sprint 4's own test list (§60).
 */
class RecurrenceCalculatorTest {

    private final RecurrenceCalculator calculator = new RecurrenceCalculator();

    private static FollowUpPlan planWith(FrequencyType frequency, LocalDate startDate) {
        return planWith(frequency, startDate, null, null);
    }

    private static FollowUpPlan planWith(FrequencyType frequency, LocalDate startDate, Integer customValue,
                                          IntervalUnit customUnit) {
        return new FollowUpPlan(null, null, null, frequency, customValue, customUnit, startDate, null,
                LocalTime.of(9, 0), 30, null, MeetingType.PARENT_MEETING, Set.of());
    }

    @Test
    void weekly() {
        FollowUpPlan plan = planWith(FrequencyType.WEEKLY, LocalDate.of(2026, 9, 5));

        assertThat(calculator.occurrenceDate(plan, 0)).isEqualTo(LocalDate.of(2026, 9, 5));
        assertThat(calculator.occurrenceDate(plan, 1)).isEqualTo(LocalDate.of(2026, 9, 12));
        assertThat(calculator.occurrenceDate(plan, 4)).isEqualTo(LocalDate.of(2026, 10, 3));
    }

    @Test
    void biweekly() {
        FollowUpPlan plan = planWith(FrequencyType.EVERY_2_WEEKS, LocalDate.of(2026, 9, 5));

        assertThat(calculator.occurrenceDate(plan, 1)).isEqualTo(LocalDate.of(2026, 9, 19));
        assertThat(calculator.occurrenceDate(plan, 2)).isEqualTo(LocalDate.of(2026, 10, 3));
    }

    @Test
    void monthly() {
        FollowUpPlan plan = planWith(FrequencyType.MONTHLY, LocalDate.of(2026, 3, 15));

        assertThat(calculator.occurrenceDate(plan, 1)).isEqualTo(LocalDate.of(2026, 4, 15));
        assertThat(calculator.occurrenceDate(plan, 6)).isEqualTo(LocalDate.of(2026, 9, 15));
    }

    @Test
    void every3Months() {
        FollowUpPlan plan = planWith(FrequencyType.EVERY_3_MONTHS, LocalDate.of(2026, 1, 10));

        assertThat(calculator.occurrenceDate(plan, 1)).isEqualTo(LocalDate.of(2026, 4, 10));
        assertThat(calculator.occurrenceDate(plan, 2)).isEqualTo(LocalDate.of(2026, 7, 10));
    }

    @Test
    void every6Months() {
        FollowUpPlan plan = planWith(FrequencyType.EVERY_6_MONTHS, LocalDate.of(2026, 1, 10));

        assertThat(calculator.occurrenceDate(plan, 1)).isEqualTo(LocalDate.of(2026, 7, 10));
        assertThat(calculator.occurrenceDate(plan, 2)).isEqualTo(LocalDate.of(2027, 1, 10));
    }

    @Test
    void yearly() {
        FollowUpPlan plan = planWith(FrequencyType.YEARLY, LocalDate.of(2026, 5, 1));

        assertThat(calculator.occurrenceDate(plan, 1)).isEqualTo(LocalDate.of(2027, 5, 1));
        assertThat(calculator.occurrenceDate(plan, 3)).isEqualTo(LocalDate.of(2029, 5, 1));
    }

    @Test
    void monthEndDoesNotDriftAcrossOccurrences() {
        // Jan 31 -> Feb (clamped to 28/29) -> Mar must land back on 31, not
        // drift to 28 (which a naive "chain plusMonths(1) from the previous
        // result" implementation would produce).
        FollowUpPlan plan = planWith(FrequencyType.MONTHLY, LocalDate.of(2026, 1, 31));

        assertThat(calculator.occurrenceDate(plan, 0)).isEqualTo(LocalDate.of(2026, 1, 31));
        assertThat(calculator.occurrenceDate(plan, 1)).isEqualTo(LocalDate.of(2026, 2, 28)); // 2026 not a leap year
        assertThat(calculator.occurrenceDate(plan, 2)).isEqualTo(LocalDate.of(2026, 3, 31)); // back to 31, no drift
        assertThat(calculator.occurrenceDate(plan, 3)).isEqualTo(LocalDate.of(2026, 4, 30));
        assertThat(calculator.occurrenceDate(plan, 4)).isEqualTo(LocalDate.of(2026, 5, 31));
    }

    @Test
    void leapYearFeb29HandledAndRecoveredInFutureLeapYears() {
        // 2028 is a leap year (Feb 29 exists); yearly recurrence should
        // clamp to Feb 28 in non-leap years but land back on Feb 29 in the
        // next leap year (2032), not drift to Feb 28 permanently.
        FollowUpPlan plan = planWith(FrequencyType.YEARLY, LocalDate.of(2028, 2, 29));

        assertThat(calculator.occurrenceDate(plan, 0)).isEqualTo(LocalDate.of(2028, 2, 29));
        assertThat(calculator.occurrenceDate(plan, 1)).isEqualTo(LocalDate.of(2029, 2, 28));
        assertThat(calculator.occurrenceDate(plan, 4)).isEqualTo(LocalDate.of(2032, 2, 29));
    }

    @Test
    void customDays() {
        FollowUpPlan plan = planWith(FrequencyType.CUSTOM, LocalDate.of(2026, 9, 1), 10, IntervalUnit.DAYS);

        assertThat(calculator.occurrenceDate(plan, 1)).isEqualTo(LocalDate.of(2026, 9, 11));
        assertThat(calculator.occurrenceDate(plan, 2)).isEqualTo(LocalDate.of(2026, 9, 21));
    }

    @Test
    void customWeeks() {
        FollowUpPlan plan = planWith(FrequencyType.CUSTOM, LocalDate.of(2026, 9, 1), 3, IntervalUnit.WEEKS);

        assertThat(calculator.occurrenceDate(plan, 1)).isEqualTo(LocalDate.of(2026, 9, 22));
    }

    @Test
    void customMonths() {
        FollowUpPlan plan = planWith(FrequencyType.CUSTOM, LocalDate.of(2026, 1, 31), 1, IntervalUnit.MONTHS);

        // Same drift-safety guarantee as the built-in MONTHLY case.
        assertThat(calculator.occurrenceDate(plan, 1)).isEqualTo(LocalDate.of(2026, 2, 28));
        assertThat(calculator.occurrenceDate(plan, 2)).isEqualTo(LocalDate.of(2026, 3, 31));
    }

    @Test
    void rejectsNegativeIndex() {
        FollowUpPlan plan = planWith(FrequencyType.WEEKLY, LocalDate.of(2026, 9, 1));

        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> calculator.occurrenceDate(plan, -1));
    }
}
