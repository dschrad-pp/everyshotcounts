-- DELETE FROM t_team WHERE id = 'fc793899-489c-4bd9-bf80-616e1318e714';
-- DELETE FROM t_organization WHERE id = 'a575815a-cbff-404e-a808-f99ecaac2ab2';
-- DELETE FROM t_contact WHERE id = '72e768e0-3842-46b5-b981-e817efbe9c8a';

-- INSERT INTO t_contact (id,contact_type,first_name,middle_name,last_name,email,telephone,birth_date,verification_code,creation_date,modification_date,created_by_id,modified_by_id,"version") VALUES
--                       ('72e768e0-3842-46b5-b981-e817efbe9c8a','ORGANIZATION','Every',NULL,'Shot Counts','esc@lektralabs.com','(515) 555-1234',72000000,'UNVERIFIED',1692634291176,1692634291176,'d79ab826-65de-4fda-8b5f-779dacfe00fe','d79ab826-65de-4fda-8b5f-779dacfe00fe',0);

-- INSERT INTO t_organization (id,"name",contact_id,type_code,status_code,creation_date,modification_date,created_by_id,modified_by_id,"version") VALUES
--                            ('a575815a-cbff-404e-a808-f99ecaac2ab2','EVERY_SHOT_COUNTS','72e768e0-3842-46b5-b981-e817efbe9c8a','ACTIVE','ACTIVE',1692634291176,1692634291176,'d79ab826-65de-4fda-8b5f-779dacfe00fe','d79ab826-65de-4fda-8b5f-779dacfe00fe',0);

-- INSERT INTO t_team (id,sport_id,organization_id,"name",description) VALUES
--                    ('fc793899-489c-4bd9-bf80-616e1318e714','e75b593b-0af6-4c80-b709-38760c578db6','a575815a-cbff-404e-a808-f99ecaac2ab2','EVERY_SHOT_COUNTS','Every Shot Counts Team #1');





