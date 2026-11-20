-- Google accounts connected through OAuth (a free Gmail works; no paid
-- Workspace needed). Two kinds:
--   * the CENTER account (center = true, user_id NULL): one shared account,
--     connected by an admin, that hosts every meeting by default;
--   * a staff member's OWN account (user_id set): optional — if connected,
--     that staff member hosts their own check-ins.

CREATE TABLE google_accounts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID UNIQUE REFERENCES users (id) ON DELETE CASCADE,
    center BOOLEAN NOT NULL DEFAULT FALSE,
    google_email VARCHAR(255) NOT NULL,
    -- OAuth refresh token, AES-GCM encrypted (see TokenCipher). Never logged or returned by the API.
    refresh_token_enc TEXT NOT NULL,
    scopes TEXT,
    -- Set when Google rejects the token (revoked / expired); someone must reconnect.
    last_error TEXT,
    connected_by UUID REFERENCES users (id),
    connected_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CHECK ((center AND user_id IS NULL) OR (NOT center AND user_id IS NOT NULL))
);

-- At most one center account.
CREATE UNIQUE INDEX uq_google_accounts_center ON google_accounts (center) WHERE center;

-- Which Google account holds the event, so it can be moved or deleted
-- later with that same account.
ALTER TABLE online_meetings ADD COLUMN google_account_id UUID REFERENCES google_accounts (id);
