package com.school.actions.model;

import com.school.followup.model.FollowUp;
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
import java.time.LocalDate;
import java.util.UUID;

/**
 * A task raised during a check-in (full-product-plan.md §15). Created only
 * by the check-in completion flow for now; there's no standalone create.
 */
@Entity
@Table(name = "action_items")
public class ActionItem {

    @Id
    @UuidGenerator
    @Column(updatable = false, nullable = false)
    private UUID id;

    // EAGER: see "Entity association fetching" in docs/ARCHITECTURE.md.
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "follow_up_id", nullable = false)
    private FollowUp followUp;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @Column(name = "assigned_to")
    private UUID assignedTo;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "text")
    private String description;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ActionPriority priority = ActionPriority.MEDIUM;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ActionItemStatus status = ActionItemStatus.TODO;

    @Column(name = "completed_at")
    private Instant completedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected ActionItem() {
        // JPA
    }

    public ActionItem(FollowUp followUp, Student student, UUID assignedTo, String title, String description,
                       LocalDate dueDate, ActionPriority priority) {
        this.followUp = followUp;
        this.student = student;
        this.assignedTo = assignedTo;
        this.title = title;
        this.description = description;
        this.dueDate = dueDate;
        if (priority != null) {
            this.priority = priority;
        }
    }

    public void markDone() {
        this.status = ActionItemStatus.DONE;
        this.completedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public FollowUp getFollowUp() {
        return followUp;
    }

    public Student getStudent() {
        return student;
    }

    public UUID getAssignedTo() {
        return assignedTo;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public ActionPriority getPriority() {
        return priority;
    }

    public ActionItemStatus getStatus() {
        return status;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
