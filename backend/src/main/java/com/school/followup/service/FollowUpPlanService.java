package com.school.followup.service;

import com.school.auth.repository.UserRepository;
import com.school.common.exception.BadRequestException;
import com.school.common.exception.ResourceNotFoundException;
import com.school.followup.dto.FollowUpPlanRequest;
import com.school.followup.model.FollowUpPlan;
import com.school.followup.model.FrequencyType;
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

import java.util.HashSet;
import java.util.UUID;

import static com.school.followup.repository.FollowUpPlanSpecifications.hasAssignedStaff;
import static com.school.followup.repository.FollowUpPlanSpecifications.hasStudent;
import static com.school.followup.repository.FollowUpPlanSpecifications.isActive;

@Service
@Transactional(readOnly = true)
public class FollowUpPlanService {

    private final FollowUpPlanRepository followUpPlanRepository;
    private final StudentRepository studentRepository;
    private final ParentRepository parentRepository;
    private final UserRepository userRepository;
    private final FollowUpRepository followUpRepository;
    private final FollowUpGenerationService generationService;

    public FollowUpPlanService(FollowUpPlanRepository followUpPlanRepository, StudentRepository studentRepository,
                                ParentRepository parentRepository, UserRepository userRepository,
                                FollowUpRepository followUpRepository, FollowUpGenerationService generationService) {
        this.followUpPlanRepository = followUpPlanRepository;
        this.studentRepository = studentRepository;
        this.parentRepository = parentRepository;
        this.userRepository = userRepository;
        this.followUpRepository = followUpRepository;
        this.generationService = generationService;
    }

    public Page<FollowUpPlan> list(UUID studentId, Boolean active, UUID assignedStaffId, Pageable pageable) {
        Specification<FollowUpPlan> spec = Specification.allOf(
                hasStudent(studentId),
                isActive(active),
                hasAssignedStaff(assignedStaffId));
        return followUpPlanRepository.findAll(spec, pageable);
    }

    public FollowUpPlan get(UUID id) {
        return followUpPlanRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No follow-up plan with id " + id));
    }

    @Transactional
    public FollowUpPlan create(FollowUpPlanRequest request) {
        Student student = resolveStudent(request.studentId());
        Parent parent = resolveParent(request.parentId(), student);
        validateCustomInterval(request);
        validateAssignedStaff(request.assignedStaffId());

        FollowUpPlan plan = new FollowUpPlan(
                student, parent, request.assignedStaffId(), request.frequencyType(),
                request.customIntervalValue(), request.customIntervalUnit(), request.startDate(),
                request.preferredDay(), request.preferredTime(),
                request.meetingDurationMinutes() != null ? request.meetingDurationMinutes() : 30,
                request.location(), request.meetingType(),
                request.reminderOffsets() != null ? request.reminderOffsets() : new HashSet<>());
        if (request.active() != null) {
            plan.setActive(request.active());
        }
        plan = followUpPlanRepository.save(plan);
        generationService.generateUpcoming(plan);
        return plan;
    }

    @Transactional
    public FollowUpPlan update(UUID id, FollowUpPlanRequest request) {
        FollowUpPlan plan = get(id);
        Student student = resolveStudent(request.studentId());
        Parent parent = resolveParent(request.parentId(), student);
        validateCustomInterval(request);
        validateAssignedStaff(request.assignedStaffId());

        plan.setStudent(student);
        plan.setParent(parent);
        plan.setAssignedStaffId(request.assignedStaffId());
        plan.setFrequencyType(request.frequencyType());
        plan.setCustomIntervalValue(request.customIntervalValue());
        plan.setCustomIntervalUnit(request.customIntervalUnit());
        plan.setStartDate(request.startDate());
        plan.setPreferredDay(request.preferredDay());
        plan.setPreferredTime(request.preferredTime());
        plan.setMeetingDurationMinutes(
                request.meetingDurationMinutes() != null ? request.meetingDurationMinutes() : 30);
        plan.setLocation(request.location());
        plan.setMeetingType(request.meetingType());
        plan.setReminderOffsets(request.reminderOffsets() != null ? request.reminderOffsets() : new HashSet<>());
        if (request.active() != null) {
            plan.setActive(request.active());
        }
        generationService.regenerateFuture(plan);
        return plan;
    }

    @Transactional
    public void delete(UUID id) {
        FollowUpPlan plan = get(id);
        if (followUpRepository.existsByFollowUpPlanId(id)) {
            throw new BadRequestException(
                    "Cannot delete a follow-up plan with existing follow-ups — deactivate it instead "
                            + "(PUT with active=false) to preserve history.");
        }
        followUpPlanRepository.delete(plan);
    }

    private Student resolveStudent(UUID studentId) {
        return studentRepository.findById(studentId)
                .orElseThrow(() -> new BadRequestException("No student with id " + studentId));
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

    private void validateAssignedStaff(UUID staffId) {
        if (staffId != null && !userRepository.existsById(staffId)) {
            throw new BadRequestException("No staff member with id " + staffId);
        }
    }

    private void validateCustomInterval(FollowUpPlanRequest request) {
        if (request.frequencyType() == FrequencyType.CUSTOM
                && (request.customIntervalValue() == null || request.customIntervalUnit() == null)) {
            throw new BadRequestException("customIntervalValue and customIntervalUnit are required when "
                    + "frequencyType is CUSTOM");
        }
    }
}
