package com.school.meetings.google;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.util.UUID;

/**
 * A connected Google account: either the shared CENTER account
 * ({@code center = true}, no user) or one staff member's own account.
 */
@Entity
@Table(name = "google_accounts")
public class GoogleAccount {

    @Id
    @UuidGenerator
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Column(name = "user_id", unique = true)
    private UUID userId;

    @Column(nullable = false)
    private boolean center;

    @Column(name = "google_email", nullable = false)
    private String googleEmail;

    @Column(name = "refresh_token_enc", nullable = false)
    private String refreshTokenEnc;

    private String scopes;

    @Column(name = "last_error")
    private String lastError;

    @Column(name = "connected_by")
    private UUID connectedBy;

    @CreationTimestamp
    @Column(name = "connected_at", nullable = false, updatable = false)
    private Instant connectedAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected GoogleAccount() {
        // JPA
    }

    public static GoogleAccount centerAccount() {
        GoogleAccount a = new GoogleAccount();
        a.center = true;
        return a;
    }

    public static GoogleAccount personal(UUID userId) {
        GoogleAccount a = new GoogleAccount();
        a.userId = userId;
        return a;
    }

    public void connect(String googleEmail, String refreshTokenEnc, String scopes, UUID connectedBy) {
        this.googleEmail = googleEmail;
        this.refreshTokenEnc = refreshTokenEnc;
        this.scopes = scopes;
        this.connectedBy = connectedBy;
        this.lastError = null;
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public boolean isCenter() {
        return center;
    }

    public String getGoogleEmail() {
        return googleEmail;
    }

    public String getRefreshTokenEnc() {
        return refreshTokenEnc;
    }

    public String getLastError() {
        return lastError;
    }

    public void setLastError(String lastError) {
        this.lastError = lastError;
    }

    public Instant getConnectedAt() {
        return connectedAt;
    }
}
