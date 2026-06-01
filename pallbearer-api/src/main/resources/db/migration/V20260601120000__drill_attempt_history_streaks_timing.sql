ALTER TABLE t_drill_attempt_history ADD COLUMN hot_streak INTEGER;
ALTER TABLE t_drill_attempt_history ADD COLUMN cold_streak INTEGER;
ALTER TABLE t_drill_attempt_history ADD COLUMN started_at TIMESTAMP;
