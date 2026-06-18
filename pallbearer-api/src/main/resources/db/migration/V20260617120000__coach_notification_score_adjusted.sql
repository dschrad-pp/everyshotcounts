ALTER TABLE t_coach_notification
  ADD COLUMN IF NOT EXISTS score_adjusted    BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE t_coach_notification
  ADD COLUMN IF NOT EXISTS makes_detected    INTEGER NOT NULL DEFAULT 0;
ALTER TABLE t_coach_notification
  ADD COLUMN IF NOT EXISTS attempts_detected INTEGER NOT NULL DEFAULT 0;
ALTER TABLE t_coach_notification
  ADD COLUMN IF NOT EXISTS makes_reported    INTEGER NOT NULL DEFAULT 0;
ALTER TABLE t_coach_notification
  ADD COLUMN IF NOT EXISTS attempts_reported INTEGER NOT NULL DEFAULT 0;
