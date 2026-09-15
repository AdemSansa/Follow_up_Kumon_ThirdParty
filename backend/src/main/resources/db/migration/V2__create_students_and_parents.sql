CREATE TABLE parents (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    phone VARCHAR(30),
    email VARCHAR(255),
    relationship VARCHAR(50) NOT NULL,
    preferred_contact_method VARCHAR(20) NOT NULL,
    notes TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_parents_last_name ON parents (last_name);

CREATE TABLE students (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    date_of_birth DATE,
    gender VARCHAR(20),
    phone VARCHAR(30),
    email VARCHAR(255),
    address VARCHAR(255),
    enrollment_date DATE NOT NULL DEFAULT CURRENT_DATE,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    level VARCHAR(50),
    assigned_teacher_id UUID REFERENCES users (id),
    notes TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_students_status ON students (status);
CREATE INDEX idx_students_assigned_teacher ON students (assigned_teacher_id);
CREATE INDEX idx_students_last_name ON students (last_name);

CREATE TABLE student_subjects (
    student_id UUID NOT NULL REFERENCES students (id) ON DELETE CASCADE,
    subject VARCHAR(20) NOT NULL,
    PRIMARY KEY (student_id, subject)
);

CREATE TABLE student_parents (
    student_id UUID NOT NULL REFERENCES students (id) ON DELETE CASCADE,
    parent_id UUID NOT NULL REFERENCES parents (id) ON DELETE CASCADE,
    PRIMARY KEY (student_id, parent_id)
);

CREATE INDEX idx_student_parents_parent ON student_parents (parent_id);
