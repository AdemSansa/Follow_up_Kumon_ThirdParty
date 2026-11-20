-- Optional fixed number of check-ins per follow-up plan (e.g. "this student
-- needs 10 check-ins"). NULL keeps the plan ongoing, as before.
ALTER TABLE follow_up_plans
    ADD COLUMN occurrence_count INT CHECK (occurrence_count BETWEEN 1 AND 200);
