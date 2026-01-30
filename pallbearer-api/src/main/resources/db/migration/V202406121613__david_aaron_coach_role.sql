-- --
-- -- Assign David and Aaron to the coach role for testing

-- -- Aaron
-- DELETE FROM t_user_role_xref
--  WHERE user_id = '18c01088-4a98-44c3-b19e-580d1e5b6809';

-- INSERT INTO t_user_role_xref (
--   user_id,
--   role_id
-- ) VALUES (
--   '18c01088-4a98-44c3-b19e-580d1e5b6809',
--   '840d3e48-340d-45a0-85ca-4ddf01dc0877'
-- );

-- -- David
-- DELETE FROM t_user_role_xref
--  WHERE user_id = '9d2adb1c-c1a8-4192-8963-83e7bc6859f5';

-- INSERT INTO t_user_role_xref (
--   user_id,
--   role_id
-- ) VALUES (
--   '9d2adb1c-c1a8-4192-8963-83e7bc6859f5',
--   '840d3e48-340d-45a0-85ca-4ddf01dc0877'
-- );
