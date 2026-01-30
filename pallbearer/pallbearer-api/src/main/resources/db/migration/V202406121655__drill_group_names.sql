-- --
-- -- Update the drill group names to reflect requested changes
-- --
-- -- Add a drill group
-- --

-- update t_drill_group
--    set name = 'Beginner'
--  where id = 'aac04c9c-b71a-47af-8cc2-861ce4cd5acd';

-- update t_drill_group
--    set name = 'Intermediate'
--  where id = '6ccc50a3-f356-416e-98db-ed50e58e976b';

-- update t_drill_group
--    set name = 'Advance'
--  where id = 'ad8903ec-e765-4f0e-a6a2-359337f37b82';

-- DELETE FROM t_drill_group
--  WHERE id = '4f60b437-18d4-44b4-a975-574eea5acc88';

-- INSERT INTO t_drill_group (
--   id,
--   team_id,
--   "name",
--   description,
--   drill_group_order,
--   drill_group_type_code,
--   creation_date,
--   modification_date,
--   created_by_id,
--   modified_by_id,
--   "version"
-- ) VALUES (
--   '4f60b437-18d4-44b4-a975-574eea5acc88'::uuid,
--   '93c71afc-552c-44fc-99bb-c9ff95b24233'::uuid,
--   'Elite',
--   NULL,
--   4,
--   'DRILL',
--   1716336829396,
--   1716336829396,
--   'd79ab826-65de-4fda-8b5f-779dacfe00fe'::uuid,
--   'd79ab826-65de-4fda-8b5f-779dacfe00fe'::uuid,
--   0
-- );
