package com.school.actions.repository;

import com.school.actions.model.ActionItem;
import com.school.actions.model.ActionItemStatus;
import org.springframework.data.jpa.domain.Specification;

import java.util.UUID;

public final class ActionItemSpecifications {

    private ActionItemSpecifications() {
    }

    public static Specification<ActionItem> hasStudent(UUID studentId) {
        return (root, query, cb) -> studentId == null ? null : cb.equal(root.get("student").get("id"), studentId);
    }

    public static Specification<ActionItem> hasFollowUp(UUID followUpId) {
        return (root, query, cb) -> followUpId == null ? null
                : cb.equal(root.get("followUp").get("id"), followUpId);
    }

    public static Specification<ActionItem> hasStatus(ActionItemStatus status) {
        return (root, query, cb) -> status == null ? null : cb.equal(root.get("status"), status);
    }

    public static Specification<ActionItem> assignedTo(UUID staffId) {
        return (root, query, cb) -> staffId == null ? null : cb.equal(root.get("assignedTo"), staffId);
    }
}
