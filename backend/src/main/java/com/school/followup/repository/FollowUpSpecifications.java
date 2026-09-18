package com.school.followup.repository;

import com.school.followup.model.FollowUp;
import com.school.followup.model.FollowUpStatus;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.UUID;

public final class FollowUpSpecifications {

    private FollowUpSpecifications() {
    }

    public static Specification<FollowUp> hasStudent(UUID studentId) {
        return (root, query, cb) -> studentId == null ? null : cb.equal(root.get("student").get("id"), studentId);
    }

    public static Specification<FollowUp> hasPlan(UUID planId) {
        return (root, query, cb) -> planId == null ? null : cb.equal(root.get("followUpPlan").get("id"), planId);
    }

    public static Specification<FollowUp> hasStatus(FollowUpStatus status) {
        return (root, query, cb) -> status == null ? null : cb.equal(root.get("status"), status);
    }

    public static Specification<FollowUp> hasAssignedStaff(UUID staffId) {
        return (root, query, cb) -> staffId == null ? null : cb.equal(root.get("assignedStaffId"), staffId);
    }

    /**
     * Calendar views (day/week/month — Sprint 5) query a visible window
     * instead of paging: any occurrence whose [scheduledStart,
     * scheduledEnd) overlaps [from, to) is included, so a follow-up that
     * starts before midnight and runs past it still shows up on both days.
     */
    public static Specification<FollowUp> overlapsRange(LocalDateTime from, LocalDateTime to) {
        return (root, query, cb) -> {
            if (from == null && to == null) {
                return null;
            }
            Predicate startsBeforeRangeEnds = to == null ? null : cb.lessThan(root.get("scheduledStart"), to);
            Predicate endsAfterRangeStarts = from == null ? null : cb.greaterThan(root.get("scheduledEnd"), from);
            if (startsBeforeRangeEnds == null) {
                return endsAfterRangeStarts;
            }
            if (endsAfterRangeStarts == null) {
                return startsBeforeRangeEnds;
            }
            return cb.and(startsBeforeRangeEnds, endsAfterRangeStarts);
        };
    }

    /**
     * scheduledEnd is in the past and the follow-up was never resolved —
     * see "Overdue logic" in docs/ARCHITECTURE.md. Computed at query time
     * rather than a stored status: no background job exists yet
     * (that's Sprint 7), so a persisted OVERDUE status could go stale.
     */
    public static Specification<FollowUp> isOverdue(Boolean overdue) {
        return (root, query, cb) -> {
            if (overdue == null) {
                return null;
            }
            Predicate unresolved = root.get("status").in(FollowUpStatus.SCHEDULED, FollowUpStatus.CONFIRMED,
                    FollowUpStatus.RESCHEDULED);
            Predicate pastEnd = cb.lessThan(root.get("scheduledEnd"), LocalDateTime.now());
            Predicate overduePredicate = cb.and(unresolved, pastEnd);
            return overdue ? overduePredicate : cb.not(overduePredicate);
        };
    }
}
