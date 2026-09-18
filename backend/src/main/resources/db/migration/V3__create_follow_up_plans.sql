CREATE TABLE follow_up_plans (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    student_id UUID NOT NULL REFERENCES students (id) ON DELETE CASCADE,
    parent_id UUID REFERENCES parents (id),
    assigned_staff_id UUID REFERENCES users (id),

    frequency_type VARCHAR(20) NOT NULL,
    custom_interval_value INT,
    custom_interval_unit VARCHAR(10),

    start_date DATE NOT NULL,
    preferred_day VARCHAR(10),
    preferred_time TIME,
    meeting_duration_minutes INT NOT NULL DEFAULT 30,
    location VARCHAR(255),
    meeting_type VARCHAR(20) NOT NULL,

    active BOOLEAN NOT NULL DEFAULT TRUE,
    next_follow_up_date DATE,

    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_follow_up_plans_student ON follow_up_plans (student_id);
CREATE INDEX idx_follow_up_plans_active ON follow_up_plans (active);
CREATE INDEX idx_follow_up_plans_assigned_staff ON follow_up_plans (assigned_staff_id);

CREATE TABLE follow_up_plan_reminders (
    follow_up_plan_id UUID NOT NULL REFERENCES follow_up_plans (id) ON DELETE CASCADE,
    reminder_offset VARCHAR(30) NOT NULL,
    PRIMARY KEY (follow_up_plan_id, reminder_offset)
);
