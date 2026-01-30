-- DROP TABLE IF EXISTS t_organization_user_xref CASCADE;

-- -- A user should belong to one organization / team
-- CREATE TABLE t_organization_user_xref (
--     organization_id    UUID NOT NULL REFERENCES t_organization(id) DEFERRABLE INITIALLY DEFERRED,
--     user_id            UUID NOT NULL REFERENCES t_user(id) DEFERRABLE INITIALLY DEFERRED,
--     team_id            UUID NOT NULL REFERENCES t_team(id) DEFERRABLE INITIALLY DEFERRED,
--     verification_code  VARCHAR(40) DEFAULT 'UNVERIFIED' NOT NULL
-- );

-- --SELECT 'a575815a-cbff-404e-a808-f99ecaac2ab2' AS organization_id
-- --      , id AS user_id
-- --      , username AS username
-- --      , 'fc793899-489c-4bd9-bf80-616e1318e714' AS team_id
-- --      , 'VERIFIED' AS verification_code
-- --FROM t_user

-- -- //GKKKKKKKK

-- -- INSERT INTO t_organization_user_xref (organization_id,user_id,team_id,verification_code) VALUES
-- -- 	 ('a575815a-cbff-404e-a808-f99ecaac2ab2','ee6dae94-5c21-4aaf-888f-e2fb4de7fcec','fc793899-489c-4bd9-bf80-616e1318e714','VERIFIED'),
-- -- 	 ('a575815a-cbff-404e-a808-f99ecaac2ab2','7a188174-a75b-495a-8d60-fa4f7b63ed5e','fc793899-489c-4bd9-bf80-616e1318e714','VERIFIED'),
-- -- 	 ('a575815a-cbff-404e-a808-f99ecaac2ab2','18c01088-4a98-44c3-b19e-580d1e5b6809','fc793899-489c-4bd9-bf80-616e1318e714','VERIFIED'),
-- -- 	 ('a575815a-cbff-404e-a808-f99ecaac2ab2','9d2adb1c-c1a8-4192-8963-83e7bc6859f5','fc793899-489c-4bd9-bf80-616e1318e714','VERIFIED'),
-- -- 	 ('a575815a-cbff-404e-a808-f99ecaac2ab2','077c002f-a530-45f4-8eb3-f748a3cebca1','fc793899-489c-4bd9-bf80-616e1318e714','VERIFIED');
