package com.school.actions.repository;

import com.school.actions.model.ActionItem;
import com.school.actions.model.ActionItemStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Collection;
import java.util.UUID;

public interface ActionItemRepository extends JpaRepository<ActionItem, UUID>, JpaSpecificationExecutor<ActionItem> {

    long countByStudentIdAndStatusIn(UUID studentId, Collection<ActionItemStatus> statuses);
}
