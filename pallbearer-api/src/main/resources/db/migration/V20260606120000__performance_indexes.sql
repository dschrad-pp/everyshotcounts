CREATE INDEX IF NOT EXISTS idx_drill_user_id ON t_drill(user_id);
CREATE INDEX IF NOT EXISTS idx_drill_attempt_history_drill_id ON t_drill_attempt_history(drill_id);
