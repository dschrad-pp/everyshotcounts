-- Add metadata column to t_user for storing JSON data

ALTER TABLE t_user
    ADD COLUMN IF NOT EXISTS metadata TEXT NOT NULL DEFAULT '';
