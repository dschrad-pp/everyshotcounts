-- --
-- -- Adds level_test column to t_drill_item. This is a boolean column that
-- -- indicates if the drill item is test item at the specified level. For
-- -- example the drill group: Beginner has level 1 (test: false), level 2
-- -- (test: false), level 2 (test: true). The drill items with level true
-- -- are a end of level test for level 2

-- ALTER TABLE t_drill_item
--   ADD COLUMN IF NOT EXISTS level_test BOOLEAN NOT NULL DEFAULT FALSE;