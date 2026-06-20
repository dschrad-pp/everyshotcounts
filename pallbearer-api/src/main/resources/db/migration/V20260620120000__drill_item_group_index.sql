-- Index supporting the difficulty-scoped skill-breakdown coverage calculation
-- (getSkillBreakdownByDifficulty), which filters t_drill_item by drill_group_id.
CREATE INDEX IF NOT EXISTS idx_drill_item_drill_group_id ON t_drill_item(drill_group_id);
