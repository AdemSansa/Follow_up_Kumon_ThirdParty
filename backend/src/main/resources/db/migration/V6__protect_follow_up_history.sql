-- Business rule 1: historical follow-ups are never deleted.
-- V4/V5 declared ON DELETE CASCADE from students, so deleting a student
-- silently erased their follow-ups and action items. Switch those foreign
-- keys to RESTRICT so the database itself refuses; the service layer
-- already rejects deleting a student who has follow-ups.

ALTER TABLE follow_ups
    DROP CONSTRAINT follow_ups_student_id_fkey,
    ADD CONSTRAINT follow_ups_student_id_fkey
        FOREIGN KEY (student_id) REFERENCES students (id) ON DELETE RESTRICT;

ALTER TABLE action_items
    DROP CONSTRAINT action_items_student_id_fkey,
    ADD CONSTRAINT action_items_student_id_fkey
        FOREIGN KEY (student_id) REFERENCES students (id) ON DELETE RESTRICT;
