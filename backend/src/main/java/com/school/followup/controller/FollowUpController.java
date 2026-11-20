package com.school.followup.controller;

import com.school.common.dto.PageResponse;
import com.school.followup.dto.AgendaItemResponse;
import com.school.followup.dto.CompleteFollowUpRequest;
import com.school.followup.dto.FollowUpRequest;
import com.school.followup.dto.FollowUpResponse;
import com.school.followup.dto.RescheduleRequest;
import com.school.followup.model.FollowUpStatus;
import com.school.followup.service.AgendaService;
import com.school.followup.service.CheckInService;
import com.school.followup.service.FollowUpService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * No DELETE endpoint here by design: "historical follow-ups must never be
 * deleted accidentally" (business rule 1, docs/PROJECT_SCOPE.md) —
 * cancel() is the only terminal state short of completion.
 */
@RestController
@RequestMapping("/api/follow-ups")
public class FollowUpController {

    private final FollowUpService followUpService;
    private final CheckInService checkInService;
    private final AgendaService agendaService;

    /**
     * A day's check-ins (default: today) with contact, student, staff and
     * plan details — powers the dashboard's "Today" list.
     */
    @GetMapping("/agenda")
    public List<AgendaItemResponse> agenda(@RequestParam(required = false) LocalDate date) {
        return agendaService.forDay(date != null ? date : LocalDate.now());
    }

    public FollowUpController(FollowUpService followUpService, CheckInService checkInService,
                              AgendaService agendaService) {
        this.agendaService = agendaService;
        this.followUpService = followUpService;
        this.checkInService = checkInService;
    }

    @GetMapping
    public PageResponse<FollowUpResponse> list(@RequestParam(required = false) UUID studentId,
                                                @RequestParam(required = false) UUID followUpPlanId,
                                                @RequestParam(required = false) FollowUpStatus status,
                                                @RequestParam(required = false) UUID assignedStaffId,
                                                @RequestParam(required = false) Boolean overdue,
                                                @RequestParam(required = false) LocalDate from,
                                                @RequestParam(required = false) LocalDate to,
                                                @PageableDefault(size = 20, sort = "scheduledStart")
                                                Pageable pageable) {
        return PageResponse.of(followUpService.list(studentId, followUpPlanId, status, assignedStaffId, overdue,
                from, to, pageable).map(FollowUpResponse::from));
    }

    @GetMapping("/{id}")
    public FollowUpResponse get(@PathVariable UUID id) {
        return FollowUpResponse.from(followUpService.get(id));
    }

    @PostMapping
    public ResponseEntity<FollowUpResponse> create(@Valid @RequestBody FollowUpRequest request) {
        FollowUpResponse created = FollowUpResponse.from(followUpService.create(request));
        return ResponseEntity.status(201).body(created);
    }

    @PostMapping("/{id}/reschedule")
    public FollowUpResponse reschedule(@PathVariable UUID id, @Valid @RequestBody RescheduleRequest request) {
        return FollowUpResponse.from(followUpService.reschedule(id, request));
    }

    @PostMapping("/{id}/complete")
    public FollowUpResponse complete(@PathVariable UUID id, @Valid @RequestBody CompleteFollowUpRequest request,
                                      Authentication authentication) {
        return FollowUpResponse.from(checkInService.complete(id, request, authentication.getName()));
    }

    @PostMapping("/{id}/cancel")
    public FollowUpResponse cancel(@PathVariable UUID id) {
        return FollowUpResponse.from(followUpService.cancel(id));
    }
}
