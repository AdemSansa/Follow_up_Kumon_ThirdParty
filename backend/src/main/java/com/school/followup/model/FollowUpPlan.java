package com.school.followup.model;

import com.school.parents.model.Parent;
import com.school.students.model.Student;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
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

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * The recurrence RULE for a student's follow-ups — not a scheduled event
 * itself. See docs/PROJECT_SCOPE.md "Core concept" and
 * docs/ARCHITECTURE.md. Generating actual FollowUp occurrences from this
 * plan is the Sprint 4 recurrence engine, not implemented yet.
 */
@Entity
@Table(name = "follow_up_plans")
public class FollowUpPlan {

    @Id
    @UuidGenerator
    @Column(updatable = false, nullable = false)
    private UUID id;

    // EAGER: FollowUpPlanResponse.from() reads student/parent fields after
    // the service's transaction has closed (mapped in the controller, same
    // pattern StudentController/ParentController use) — LAZY here throws
    // LazyInitializationException outside the session.
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "parent_id")
    private Parent parent;

    @Column(name = "assigned_staff_id")
    private UUID assignedStaffId;

    @Enumerated(EnumType.STRING)
    @Column(name = "frequency_type", nullable = false)
    private FrequencyType frequencyType;

    @Column(name = "custom_interval_value")
    private Integer customIntervalValue;

    @Enumerated(EnumType.STRING)
    @Column(name = "custom_interval_unit")
    private IntervalUnit customIntervalUnit;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "preferred_day")
    private DayOfWeek preferredDay;

    @Column(name = "preferred_time")
    private LocalTime preferredTime;

    @Column(name = "meeting_duration_minutes", nullable = false)
    private int meetingDurationMinutes = 30;

    private String location;

    @Enumerated(EnumType.STRING)
    @Column(name = "meeting_type", nullable = false)
    private MeetingType meetingType;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "next_follow_up_date")
    private LocalDate nextFollowUpDate;

    /** Total number of check-ins this plan produces; null = ongoing. */
    @Column(name = "occurrence_count")
    private Integer occurrenceCount;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "follow_up_plan_reminders", joinColumns = @JoinColumn(name = "follow_up_plan_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "reminder_offset")
    private Set<ReminderOffset> reminderOffsets = new HashSet<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected FollowUpPlan() {
        // JPA
    }

    public FollowUpPlan(Student student, Parent parent, UUID assignedStaffId, FrequencyType frequencyType,
                         Integer customIntervalValue, IntervalUnit customIntervalUnit, LocalDate startDate,
                         DayOfWeek preferredDay, LocalTime preferredTime, int meetingDurationMinutes,
                         String location, MeetingType meetingType, Set<ReminderOffset> reminderOffsets) {
        this.student = student;
        this.parent = parent;
        this.assignedStaffId = assignedStaffId;
        this.frequencyType = frequencyType;
        this.customIntervalValue = customIntervalValue;
        this.customIntervalUnit = customIntervalUnit;
        this.startDate = startDate;
        this.preferredDay = preferredDay;
        this.preferredTime = preferredTime;
        this.meetingDurationMinutes = meetingDurationMinutes;
        this.location = location;
        this.meetingType = meetingType;
        this.reminderOffsets = reminderOffsets != null ? new HashSet<>(reminderOffsets) : new HashSet<>();
        this.nextFollowUpDate = startDate;
    }

    public UUID getId() {
        return id;
    }

    public Student getStudent() {
        return student;
    }

    public void setStudent(Student student) {
        this.student = student;
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

    public FrequencyType getFrequencyType() {
        return frequencyType;
    }

    public void setFrequencyType(FrequencyType frequencyType) {
        this.frequencyType = frequencyType;
    }

    public Integer getCustomIntervalValue() {
        return customIntervalValue;
    }

    public void setCustomIntervalValue(Integer customIntervalValue) {
        this.customIntervalValue = customIntervalValue;
    }

    public IntervalUnit getCustomIntervalUnit() {
        return customIntervalUnit;
    }

    public void setCustomIntervalUnit(IntervalUnit customIntervalUnit) {
        this.customIntervalUnit = customIntervalUnit;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public DayOfWeek getPreferredDay() {
        return preferredDay;
    }

    public void setPreferredDay(DayOfWeek preferredDay) {
        this.preferredDay = preferredDay;
    }

    public LocalTime getPreferredTime() {
        return preferredTime;
    }

    public void setPreferredTime(LocalTime preferredTime) {
        this.preferredTime = preferredTime;
    }

    public int getMeetingDurationMinutes() {
        return meetingDurationMinutes;
    }

    public void setMeetingDurationMinutes(int meetingDurationMinutes) {
        this.meetingDurationMinutes = meetingDurationMinutes;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public MeetingType getMeetingType() {
        return meetingType;
    }

    public void setMeetingType(MeetingType meetingType) {
        this.meetingType = meetingType;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public LocalDate getNextFollowUpDate() {
        return nextFollowUpDate;
    }

    public void setNextFollowUpDate(LocalDate nextFollowUpDate) {
        this.nextFollowUpDate = nextFollowUpDate;
    }

    public Integer getOccurrenceCount() {
        return occurrenceCount;
    }

    public void setOccurrenceCount(Integer occurrenceCount) {
        this.occurrenceCount = occurrenceCount;
    }

    public Set<ReminderOffset> getReminderOffsets() {
        return reminderOffsets;
    }

    public void setReminderOffsets(Set<ReminderOffset> reminderOffsets) {
        this.reminderOffsets = reminderOffsets;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
