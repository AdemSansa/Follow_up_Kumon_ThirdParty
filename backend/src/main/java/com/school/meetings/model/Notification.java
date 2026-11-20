package com.school.meetings.model;

import com.school.followup.model.FollowUp;
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
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

/** One message sent (or attempted) to a parent about a follow-up. */
@Entity
@Table(name = "notifications")
public class Notification {

    @Id
    @UuidGenerator
    @Column(updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "follow_up_id", nullable = false)
    private FollowUp followUp;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationKind kind;

    @Column(nullable = false)
    private String channel = "EMAIL";

    @Column(nullable = false)
    private String recipient;

    @Column(name = "scheduled_for", nullable = false)
    private LocalDateTime scheduledFor;

    @Column(nullable = false)
    private String status;

    @Column(name = "last_error")
    private String lastError;

    @Column(name = "sent_at")
    private Instant sentAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Notification() {
        // JPA
    }

    public Notification(FollowUp followUp, NotificationKind kind, String recipient, LocalDateTime scheduledFor) {
        this.followUp = followUp;
        this.kind = kind;
        this.recipient = recipient;
        this.scheduledFor = scheduledFor;
        this.status = "PENDING";
    }

    public void markSent() {
        this.status = "SENT";
        this.sentAt = Instant.now();
        this.lastError = null;
    }

    public void markFailed(String error) {
        this.status = "FAILED";
        this.lastError = error;
    }

    public UUID getId() {
        return id;
    }

    public NotificationKind getKind() {
        return kind;
    }

    public String getRecipient() {
        return recipient;
    }

    public String getStatus() {
        return status;
    }

    public Instant getSentAt() {
        return sentAt;
    }

    public LocalDateTime getScheduledFor() {
        return scheduledFor;
    }
}
