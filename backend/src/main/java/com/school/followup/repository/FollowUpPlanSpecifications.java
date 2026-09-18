package com.school.followup.repository;

import com.school.followup.model.FollowUpPlan;
import org.springframework.data.jpa.domain.Specification;

import java.util.UUID;

public final class FollowUpPlanSpecifications {

    private FollowUpPlanSpecifications() {
    }

    public static Specification<FollowUpPlan> hasStudent(UUID studentId) {
        return (root, query, cb) -> studentId == null ? null : cb.equal(root.get("student").get("id"), studentId);
    }

    public static Specification<FollowUpPlan> isActive(Boolean active) {
        return (root, query, cb) -> active == null ? null : cb.equal(root.get("active"), active);
    }

    public static Specification<FollowUpPlan> hasAssignedStaff(UUID staffId) {
        return (root, query, cb) -> staffId == null ? null : cb.equal(root.get("assignedStaffId"), staffId);
    }
}
