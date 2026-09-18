package com.school.followup.controller;

import com.school.common.dto.PageResponse;
import com.school.followup.dto.FollowUpPlanRequest;
import com.school.followup.dto.FollowUpPlanResponse;
import com.school.followup.service.FollowUpPlanService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/follow-up-plans")
public class FollowUpPlanController {

    private final FollowUpPlanService followUpPlanService;

    public FollowUpPlanController(FollowUpPlanService followUpPlanService) {
        this.followUpPlanService = followUpPlanService;
    }

    @GetMapping
    public PageResponse<FollowUpPlanResponse> list(@RequestParam(required = false) UUID studentId,
                                                     @RequestParam(required = false) Boolean active,
                                                     @RequestParam(required = false) UUID assignedStaffId,
                                                     @PageableDefault(size = 20, sort = "startDate") Pageable pageable) {
        return PageResponse.of(followUpPlanService.list(studentId, active, assignedStaffId, pageable)
                .map(FollowUpPlanResponse::from));
    }

    @GetMapping("/{id}")
    public FollowUpPlanResponse get(@PathVariable UUID id) {
        return FollowUpPlanResponse.from(followUpPlanService.get(id));
    }

    @PostMapping
    public ResponseEntity<FollowUpPlanResponse> create(@Valid @RequestBody FollowUpPlanRequest request) {
        FollowUpPlanResponse created = FollowUpPlanResponse.from(followUpPlanService.create(request));
        return ResponseEntity.status(201).body(created);
    }

    @PutMapping("/{id}")
    public FollowUpPlanResponse update(@PathVariable UUID id, @Valid @RequestBody FollowUpPlanRequest request) {
        return FollowUpPlanResponse.from(followUpPlanService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        followUpPlanService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
