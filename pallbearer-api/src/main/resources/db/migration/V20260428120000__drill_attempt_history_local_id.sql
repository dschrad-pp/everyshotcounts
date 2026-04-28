ALTER TABLE t_drill_attempt_history
ADD COLUMN attempt_local_id VARCHAR(255);

ALTER TABLE t_drill_attempt_history
ADD CONSTRAINT uq_attempt_local_id UNIQUE (attempt_local_id);
