package com.school.actions.controller;

import com.school.actions.dto.ActionItemResponse;
import com.school.actions.model.ActionItemStatus;
import com.school.actions.service.ActionItemService;
import com.school.common.dto.PageResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Action items are created only by the check-in completion flow
 * (POST /api/follow-ups/{id}/complete); this controller just lists them
 * and marks them done.
 */
@RestController
@RequestMapping("/api/action-items")
public class ActionItemController {

    private final ActionItemService actionItemService;

    public ActionItemController(ActionItemService actionItemService) {
        this.actionItemService = actionItemService;
    }

    @GetMapping
    public PageResponse<ActionItemResponse> list(@RequestParam(required = false) UUID studentId,
                                                  @RequestParam(required = false) UUID followUpId,
                                                  @RequestParam(required = false) ActionItemStatus status,
                                                  @RequestParam(required = false) UUID assignedTo,
                                                  @PageableDefault(size = 20, sort = "dueDate") Pageable pageable) {
        return PageResponse.of(actionItemService.list(studentId, followUpId, status, assignedTo, pageable)
                .map(ActionItemResponse::from));
    }

    @PostMapping("/{id}/complete")
    public ActionItemResponse complete(@PathVariable UUID id) {
        return ActionItemResponse.from(actionItemService.markDone(id));
    }
}
