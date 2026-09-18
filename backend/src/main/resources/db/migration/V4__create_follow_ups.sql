CREATE TABLE follow_ups (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    student_id UUID NOT NULL REFERENCES students (id) ON DELETE CASCADE,
    follow_up_plan_id UUID REFERENCES follow_up_plans (id),
    parent_id UUID REFERENCES parents (id),
    assigned_staff_id UUID REFERENCES users (id),

    type VARCHAR(20) NOT NULL,
    scheduled_start TIMESTAMP NOT NULL,
    scheduled_end TIMESTAMP NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'SCHEDULED',

    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_follow_ups_student ON follow_ups (student_id);
CREATE INDEX idx_follow_ups_plan ON follow_ups (follow_up_plan_id);
CREATE INDEX idx_follow_ups_status ON follow_ups (status);
CREATE INDEX idx_follow_ups_scheduled_start ON follow_ups (scheduled_start);
