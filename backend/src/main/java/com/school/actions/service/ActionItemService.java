package com.school.actions.service;

import com.school.actions.model.ActionItem;
import com.school.actions.model.ActionItemStatus;
import com.school.actions.repository.ActionItemRepository;
import com.school.common.exception.BadRequestException;
import com.school.common.exception.ResourceNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static com.school.actions.repository.ActionItemSpecifications.assignedTo;
import static com.school.actions.repository.ActionItemSpecifications.hasFollowUp;
import static com.school.actions.repository.ActionItemSpecifications.hasStatus;
import static com.school.actions.repository.ActionItemSpecifications.hasStudent;

@Service
@Transactional(readOnly = true)
public class ActionItemService {

    private final ActionItemRepository actionItemRepository;

    public ActionItemService(ActionItemRepository actionItemRepository) {
        this.actionItemRepository = actionItemRepository;
    }

    public Page<ActionItem> list(UUID studentId, UUID followUpId, ActionItemStatus status, UUID assignedToId,
                                  Pageable pageable) {
        Specification<ActionItem> spec = Specification.allOf(
                hasStudent(studentId), hasFollowUp(followUpId), hasStatus(status), assignedTo(assignedToId));
        return actionItemRepository.findAll(spec, pageable);
    }

    @Transactional
    public ActionItem markDone(UUID id) {
        ActionItem item = actionItemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No action item with id " + id));
        if (item.getStatus() == ActionItemStatus.CANCELLED) {
            throw new BadRequestException("Cannot complete a CANCELLED action item");
        }
        if (item.getStatus() != ActionItemStatus.DONE) {
            item.markDone();
        }
        return item;
    }
}
