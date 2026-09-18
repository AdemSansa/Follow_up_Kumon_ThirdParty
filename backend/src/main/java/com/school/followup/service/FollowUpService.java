package com.school.followup.service;

import com.school.common.exception.BadRequestException;
import com.school.common.exception.ResourceNotFoundException;
import com.school.followup.dto.FollowUpRequest;
import com.school.followup.dto.RescheduleRequest;
import com.school.followup.model.FollowUp;
import com.school.followup.model.FollowUpPlan;
import com.school.followup.model.FollowUpStatus;
import com.school.followup.repository.FollowUpPlanRepository;
import com.school.followup.repository.FollowUpRepository;
import com.school.parents.model.Parent;
import com.school.parents.repository.ParentRepository;
import com.school.students.model.Student;
import com.school.students.repository.StudentRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;

import static com.school.followup.repository.FollowUpSpecifications.hasAssignedStaff;
import static com.school.followup.repository.FollowUpSpecifications.hasPlan;
import static com.school.followup.repository.FollowUpSpecifications.hasStatus;
import static com.school.followup.repository.FollowUpSpecifications.hasStudent;
import static com.school.followup.repository.FollowUpSpecifications.isOverdue;
import static com.school.followup.repository.FollowUpSpecifications.overlapsRange;

/**
 * The FollowUp instance itself: listing, ad-hoc creation, rescheduling,
 * and cancellation. Recurring-plan generation lives in
 * {@link FollowUpGenerationService}.
 */
@Service
@Transactional(readOnly = true)
public class FollowUpService {

    private final FollowUpRepository followUpRepository;
    private final FollowUpPlanRepository followUpPlanRepository;
    private final StudentRepository studentRepository;
    private final ParentRepository parentRepository;

    public FollowUpService(FollowUpRepository followUpRepository, FollowUpPlanRepository followUpPlanRepository,
                            StudentRepository studentRepository, ParentRepository parentRepository) {
        this.followUpRepository = followUpRepository;
        this.followUpPlanRepository = followUpPlanRepository;
        this.studentRepository = studentRepository;
        this.parentRepository = parentRepository;
    }

    /**
     * from/to are calendar-day bounds (inclusive on both ends from the
     * caller's point of view) — e.g. a month view passes the first and
     * last day of the visible grid. Converted here to a
     * [startOfFrom, startOfDayAfterTo) instant range so a follow-up
     * scheduled anywhere within "to" (the whole day) is still included.
     */
    public Page<FollowUp> list(UUID studentId, UUID followUpPlanId, FollowUpStatus status, UUID assignedStaffId,
                                Boolean overdue, LocalDate from, LocalDate to, Pageable pageable) {
        Specification<FollowUp> spec = Specification.allOf(
                hasStudent(studentId),
                hasPlan(followUpPlanId),
                hasStatus(status),
                hasAssignedStaff(assignedStaffId),
                isOverdue(overdue),
                overlapsRange(from == null ? null : from.atStartOfDay(),
                        to == null ? null : to.plusDays(1).atStartOfDay()));
        return followUpRepository.findAll(spec, pageable);
    }

    public FollowUp get(UUID id) {
        return followUpRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No follow-up with id " + id));
    }

    @Transactional
    public FollowUp create(FollowUpRequest request) {
        Student student = studentRepository.findById(request.studentId())
                .orElseThrow(() -> new BadRequestException("No student with id " + request.studentId()));
        FollowUpPlan plan = null;
        if (request.followUpPlanId() != null) {
            plan = followUpPlanRepository.findById(request.followUpPlanId())
                    .orElseThrow(() -> new BadRequestException("No follow-up plan with id "
                            + request.followUpPlanId()));
        }
        Parent parent = resolveParent(request.parentId(), student);
        if (!request.scheduledEnd().isAfter(request.scheduledStart())) {
            throw new BadRequestException("scheduledEnd must be after scheduledStart");
        }

        FollowUp followUp = new FollowUp(student, plan, parent, request.assignedStaffId(), request.type(),
                request.scheduledStart(), request.scheduledEnd());
        return followUpRepository.save(followUp);
    }

    /**
     * Moves one occurrence to a new time. Never touches the recurring
     * plan (business rule 3, docs/PROJECT_SCOPE.md).
     */
    @Transactional
    public FollowUp reschedule(UUID id, RescheduleRequest request) {
        FollowUp followUp = get(id);
        if (followUp.getStatus() == FollowUpStatus.CANCELLED || followUp.getStatus() == FollowUpStatus.COMPLETED) {
            throw new BadRequestException("Cannot reschedule a follow-up that is " + followUp.getStatus());
        }
        if (!request.scheduledEnd().isAfter(request.scheduledStart())) {
            throw new BadRequestException("scheduledEnd must be after scheduledStart");
        }
        followUp.setScheduledStart(request.scheduledStart());
        followUp.setScheduledEnd(request.scheduledEnd());
        followUp.setStatus(FollowUpStatus.RESCHEDULED);
        return followUp;
    }

    @Transactional
    public FollowUp cancel(UUID id) {
        FollowUp followUp = get(id);
        if (followUp.getStatus() == FollowUpStatus.COMPLETED) {
            throw new BadRequestException("Cannot cancel a follow-up that is already COMPLETED");
        }
        followUp.setStatus(FollowUpStatus.CANCELLED);
        return followUp;
    }

    private Parent resolveParent(UUID parentId, Student student) {
        if (parentId == null) {
            return null;
        }
        Parent parent = parentRepository.findById(parentId)
                .orElseThrow(() -> new BadRequestException("No parent with id " + parentId));
        boolean linkedToStudent = student.getParents().stream().anyMatch(p -> p.getId().equals(parentId));
        if (!linkedToStudent) {
            throw new BadRequestException("Parent " + parentId + " is not linked to student " + student.getId());
        }
        return parent;
    }
}
