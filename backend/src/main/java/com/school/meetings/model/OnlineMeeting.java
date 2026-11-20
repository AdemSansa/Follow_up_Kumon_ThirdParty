package com.school.meetings.model;

import com.school.followup.model.FollowUp;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.util.UUID;

/** The Google Meet (calendar event + join link) for one online follow-up. */
@Entity
@Table(name = "online_meetings")
public class OnlineMeeting {

    @Id
    @UuidGenerator
    @Column(updatable = false, nullable = false)
    private UUID id;

    @OneToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "follow_up_id", nullable = false, unique = true)
    private FollowUp followUp;

    @Column(nullable = false)
    private String provider = "GOOGLE_MEET";

    @Column(name = "provider_event_id")
    private String providerEventId;

    /** The connected Google account (google_accounts.id) whose calendar holds the event. */
    @Column(name = "google_account_id")
    private UUID googleAccountId;

    /** That account's Gmail address, for display. */
    @Column(name = "organizer_email")
    private String organizerEmail;

    @Column(name = "join_url")
    private String joinUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MeetingStatus status = MeetingStatus.PENDING;

    @Column(name = "last_error")
    private String lastError;

    @Column(nullable = false)
    private int attempts;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected OnlineMeeting() {
        // JPA
    }

    public OnlineMeeting(FollowUp followUp) {
        this.followUp = followUp;
    }

    public void markCreated(String eventId, UUID accountId, String organizer, String url) {
        this.providerEventId = eventId;
        this.googleAccountId = accountId;
        this.organizerEmail = organizer;
        this.joinUrl = url;
        this.status = MeetingStatus.CREATED;
        this.lastError = null;
        this.attempts++;
    }

    public void markFailed(String error) {
        this.status = MeetingStatus.FAILED;
        this.lastError = error;
        this.attempts++;
    }

    /** Records a problem without changing the status (e.g. the link works but a sync failed). */
    public void noteError(String error) {
        this.lastError = error;
    }

    public void markCancelled() {
        this.status = MeetingStatus.CANCELLED;
    }

    public UUID getId() {
        return id;
    }

    public FollowUp getFollowUp() {
        return followUp;
    }

    public String getProviderEventId() {
        return providerEventId;
    }

    public UUID getGoogleAccountId() {
        return googleAccountId;
    }

    public String getOrganizerEmail() {
        return organizerEmail;
    }

    public String getJoinUrl() {
        return joinUrl;
    }

    public MeetingStatus getStatus() {
        return status;
    }

    public String getLastError() {
        return lastError;
    }

    public int getAttempts() {
        return attempts;
    }
}
