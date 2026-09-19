package com.school.followup.service;

import com.school.actions.model.ActionItem;
import com.school.actions.repository.ActionItemRepository;
import com.school.auth.model.User;
import com.school.auth.repository.UserRepository;
import com.school.common.exception.BadRequestException;
import com.school.common.exception.ResourceNotFoundException;
import com.school.followup.dto.CompleteFollowUpRequest;
import com.school.followup.model.FollowUp;
import com.school.followup.model.FollowUpStatus;
import com.school.followup.repository.FollowUpRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * The check-in completion workflow (full-product-plan.md §37), scoped to
 * what exists so far: ratings/concerns/notes saved, action items created,
 * status set to COMPLETED, and the plan's rolling window topped up. Steps
 * that depend on later sprints — calendar events (external sync),
 * reminders (Sprint 9), statistics (Sprint 8), realtime events — are not
 * implemented, not stubbed.
 *
 * <p>All-or-nothing: a single transaction, so a bad action item (e.g.
 * unknown assignee) rolls back the completion too.
 */
@Service
public class CheckInService {

    private final FollowUpRepository followUpRepository;
    private final ActionItemRepository actionItemRepository;
    private final UserRepository userRepository;
    private final FollowUpGenerationService generationService;

    public CheckInService(FollowUpRepository followUpRepository, ActionItemRepository actionItemRepository,
                           UserRepository userRepository, FollowUpGenerationService generationService) {
        this.followUpRepository = followUpRepository;
        this.actionItemRepository = actionItemRepository;
        this.userRepository = userRepository;
        this.generationService = generationService;
    }

    @Transactional
    public FollowUp complete(UUID followUpId, CompleteFollowUpRequest request, String completedByEmail) {
        FollowUp followUp = followUpRepository.findById(followUpId)
                .orElseThrow(() -> new ResourceNotFoundException("No follow-up with id " + followUpId));
        if (followUp.getStatus() == FollowUpStatus.COMPLETED) {
            throw new BadRequestException("This follow-up is already completed");
        }
        if (followUp.getStatus() == FollowUpStatus.CANCELLED) {
            throw new BadRequestException("Cannot complete a CANCELLED follow-up");
        }

        UUID completedBy = userRepository.findByEmailIgnoreCase(completedByEmail).map(User::getId).orElse(null);

        followUp.complete(completedBy, request.progressRating(), request.engagementRating(),
                request.parentSatisfactionRating(), request.concerns(), request.summary(),
                request.recommendations());

        List<CompleteFollowUpRequest.ActionItemInput> inputs = request.actionItems();
        if (inputs != null) {
            for (CompleteFollowUpRequest.ActionItemInput input : inputs) {
                if (input.assignedTo() != null && !userRepository.existsById(input.assignedTo())) {
                    throw new BadRequestException("No staff member with id " + input.assignedTo());
                }
                actionItemRepository.save(new ActionItem(followUp, followUp.getStudent(), input.assignedTo(),
                        input.title(), input.description(), input.dueDate(), input.priority()));
            }
        }

        // Business rule 4 (docs/PROJECT_SCOPE.md): completing a follow-up
        // determines the next occurrence. Occurrences are pre-generated in a
        // rolling window (Sprint 4), so the next one normally already exists —
        // this tops the window back up so it keeps extending as follow-ups
        // are completed, without waiting on the Sprint 7 daily job.
        if (followUp.getFollowUpPlan() != null) {
            generationService.generateUpcoming(followUp.getFollowUpPlan());
        }

        return followUp;
    }
}
