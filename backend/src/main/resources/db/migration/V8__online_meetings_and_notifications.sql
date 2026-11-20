-- Google Meet links for online (VIDEO_CALL) follow-ups, and a log of every
-- message sent to parents. See docs/features/google-meet.md.

-- One meeting per follow-up. The database stays the source of truth
-- (business rule 10); Google only holds a copy of the event.
CREATE TABLE online_meetings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    follow_up_id UUID NOT NULL UNIQUE REFERENCES follow_ups (id),
    provider VARCHAR(20) NOT NULL DEFAULT 'GOOGLE_MEET',
    -- Google Calendar event id, and the Workspace account whose calendar
    -- holds it (needed to update/delete it as the same user later).
    provider_event_id VARCHAR(255),
    organizer_email VARCHAR(255),
    join_url VARCHAR(500),
    -- PENDING (not created yet / retrying), CREATED, FAILED, CANCELLED
    status VARCHAR(20) NOT NULL,
    last_error TEXT,
    attempts INT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_online_meetings_status ON online_meetings (status);

-- Every email the app sends about a follow-up. The unique key makes sending
-- idempotent: the same kind of message for the same scheduled time goes to
-- the same recipient at most once (a reschedule changes scheduled_for, so
-- the new time gets its own invite).
CREATE TABLE notifications (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    follow_up_id UUID NOT NULL REFERENCES follow_ups (id),
    kind VARCHAR(30) NOT NULL,       -- MEETING_INVITE, MEETING_RESCHEDULED, MEETING_CANCELLED
    channel VARCHAR(20) NOT NULL,    -- EMAIL
    recipient VARCHAR(255) NOT NULL,
    scheduled_for TIMESTAMP NOT NULL, -- the follow-up start time this message is about
    status VARCHAR(20) NOT NULL,     -- SENT, FAILED
    last_error TEXT,
    sent_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (follow_up_id, kind, recipient, scheduled_for)
);

CREATE INDEX idx_notifications_follow_up ON notifications (follow_up_id);
