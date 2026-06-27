-- Supports the athlete activity heatmap query, which scans completion rows for a
-- single athlete within a date range (filtered on user_id + recorded_at).
-- Without this, that query is a full scan of t_drill_attempt_history per profile open.
CREATE INDEX IF NOT EXISTS idx_drill_attempt_history_user_recorded
    ON t_drill_attempt_history (user_id, recorded_at);
