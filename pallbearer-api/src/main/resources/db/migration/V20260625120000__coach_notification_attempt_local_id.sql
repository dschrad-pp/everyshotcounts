-- Store the round's stable client id on the coach notification so a notification
-- can be resolved to the exact passing round it was created for (one round = one
-- video). Nullable: notifications created before this column existed have no value
-- and fall back to the most recent passing round of the drill.
ALTER TABLE t_coach_notification
ADD COLUMN attempt_local_id VARCHAR(255);
