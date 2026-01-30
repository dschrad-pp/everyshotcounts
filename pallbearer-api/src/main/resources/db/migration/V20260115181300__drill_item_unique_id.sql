-- Add unique_id column to t_drill_item_import and t_drill_item tables

ALTER TABLE t_drill_item_import
    ADD COLUMN IF NOT EXISTS unique_id VARCHAR(255);

ALTER TABLE t_drill_item
    ADD COLUMN IF NOT EXISTS unique_id VARCHAR(255);
