package com.school.followup.model;

import com.school.parents.model.Parent;
import com.school.students.model.Student;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * One concrete, scheduled occurrence — as opposed to {@link FollowUpPlan},
 * which is the recurrence rule. See docs/PROJECT_SCOPE.md "Core concept".
 * Never hard-deleted (business rule 1); see FollowUpService.
 */
@Entity
@Table(name = "follow_ups")
public class FollowUp {

    @Id
    @UuidGenerator
    @Column(updatable = false, nullable = false)
    private UUID id;

    // EAGER: see "Entity association fetching" in docs/ARCHITECTURE.md.
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "follow_up_plan_id")
    private FollowUpPlan followUpPlan;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "parent_id")
    private Parent parent;

    @Column(name = "assigned_staff_id")
    private UUID assignedStaffId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MeetingType type;

    @Column(name = "scheduled_start", nullable = false)
    private LocalDateTime scheduledStart;

    @Column(name = "scheduled_end", nullable = false)
    private LocalDateTime scheduledEnd;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FollowUpStatus status = FollowUpStatus.SCHEDULED;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected FollowUp() {
        // JPA
    }

    public FollowUp(Student student, FollowUpPlan followUpPlan, Parent parent, UUID assignedStaffId,
                     MeetingType type, LocalDateTime scheduledStart, LocalDateTime scheduledEnd) {
        this.student = student;
        this.followUpPlan = followUpPlan;
        this.parent = parent;
        this.assignedStaffId = assignedStaffId;
        this.type = type;
        this.scheduledStart = scheduledStart;
        this.scheduledEnd = scheduledEnd;
    }

    public UUID getId() {
        return id;
    }

    public Student getStudent() {
        return student;
    }

    public FollowUpPlan getFollowUpPlan() {
        return followUpPlan;
    }

    public Parent getParent() {
        return parent;
    }

    public void setParent(Parent parent) {
        this.parent = parent;
    }

    public UUID getAssignedStaffId() {
        return assignedStaffId;
    }

    public void setAssignedStaffId(UUID assignedStaffId) {
        this.assignedStaffId = assignedStaffId;
    }

    public MeetingType getType() {
        return type;
    }

    public void setType(MeetingType type) {
        this.type = type;
    }

    public LocalDateTime getScheduledStart() {
        return scheduledStart;
    }

    public void setScheduledStart(LocalDateTime scheduledStart) {
        this.scheduledStart = scheduledStart;
    }

    public LocalDateTime getScheduledEnd() {
        return scheduledEnd;
    }

    public void setScheduledEnd(LocalDateTime scheduledEnd) {
        this.scheduledEnd = scheduledEnd;
    }

    public FollowUpStatus getStatus() {
        return status;
    }

    public void setStatus(FollowUpStatus status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
