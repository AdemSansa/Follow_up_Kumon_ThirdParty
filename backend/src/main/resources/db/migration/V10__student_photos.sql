-- Optional profile picture per student. Kept in its own table so the image
-- bytes are only read when the photo itself is requested, never when a
-- student is listed or embedded in a follow-up.
CREATE TABLE student_photos (
    student_id UUID PRIMARY KEY REFERENCES students (id) ON DELETE CASCADE,
    content_type VARCHAR(50) NOT NULL,
    data BYTEA NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
