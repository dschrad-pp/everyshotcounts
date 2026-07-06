-- In-app account deletion (Apple 5.1.1(v)) — 30-day grace period.
-- "Pending deletion" = deletion_requested_at IS NOT NULL; restore nulls both columns.
-- Epoch millis, consistent with creation_date/modification_date.
ALTER TABLE t_user ADD COLUMN deletion_requested_at BIGINT NULL;
ALTER TABLE t_user ADD COLUMN purge_after BIGINT NULL;

-- The daily purge cron scans for purge_after <= now; partial index keeps it cheap
-- since almost no rows are ever pending deletion.
CREATE INDEX idx_user_purge_after ON t_user (purge_after) WHERE purge_after IS NOT NULL;
