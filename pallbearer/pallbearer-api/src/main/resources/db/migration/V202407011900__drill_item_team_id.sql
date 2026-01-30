-- --
-- -- Adds team_id to t_drill_item
-- --

-- -- ALTER TABLE t_drill_item
-- --   ADD COLUMN IF NOT EXISTS team_id UUID NOT NULL REFERENCES t_team(id) DEFERRABLE INITIALLY DEFERRED;

-- ALTER TABLE t_drill_item
-- ADD COLUMN IF NOT EXISTS team_id UUID REFERENCES t_team(id) DEFERRABLE INITIALLY DEFERRED;

