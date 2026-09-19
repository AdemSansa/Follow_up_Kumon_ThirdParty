ALTER TABLE follow_ups
    ADD COLUMN completed_at TIMESTAMPTZ,
    ADD COLUMN completed_by UUID REFERENCES users (id),
    ADD COLUMN summary TEXT,
    ADD COLUMN recommendations TEXT,
    ADD COLUMN progress_rating INT CHECK (progress_rating BETWEEN 1 AND 5),
    ADD COLUMN engagement_rating INT CHECK (engagement_rating BETWEEN 1 AND 5),
    ADD COLUMN parent_satisfaction_rating INT CHECK (parent_satisfaction_rating BETWEEN 1 AND 5);

CREATE TABLE follow_up_concerns (
    follow_up_id UUID NOT NULL REFERENCES follow_ups (id) ON DELETE CASCADE,
    concern VARCHAR(20) NOT NULL,
    PRIMARY KEY (follow_up_id, concern)
);

CREATE TABLE action_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    follow_up_id UUID NOT NULL REFERENCES follow_ups (id),
    student_id UUID NOT NULL REFERENCES students (id) ON DELETE CASCADE,
    assigned_to UUID REFERENCES users (id),
    title VARCHAR(255) NOT NULL,
    description TEXT,
    due_date DATE,
    priority VARCHAR(10) NOT NULL DEFAULT 'MEDIUM',
    status VARCHAR(20) NOT NULL DEFAULT 'TODO',
    completed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_action_items_student ON action_items (student_id);
CREATE INDEX idx_action_items_follow_up ON action_items (follow_up_id);
CREATE INDEX idx_action_items_status ON action_items (status);
