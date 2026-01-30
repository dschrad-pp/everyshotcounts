-- DELETE FROM t_team_athlete_xref WHERE team_id = '93c71afc-552c-44fc-99bb-c9ff95b24233';
-- DELETE FROM t_team WHERE id = '93c71afc-552c-44fc-99bb-c9ff95b24233';
-- DELETE FROM t_organization WHERE id = 'df0b272f-dcfb-446c-81a5-ae70222d1e49';
-- DELETE FROM t_contact WHERE id = '31fbe297-d72a-475c-bfd0-23f3277e88da';

-- INSERT INTO t_contact (id,contact_type,first_name,middle_name,last_name,email,telephone,birth_date,verification_code,creation_date,modification_date,created_by_id,modified_by_id,"version") VALUES
--                       ('31fbe297-d72a-475c-bfd0-23f3277e88da','ORGANIZATION','Every',NULL,'Shot Counts Dev','esc_dev@lektralabs.com','(515) 555-1234',72000000,'UNVERIFIED',1692634291176,1692634291176,'d79ab826-65de-4fda-8b5f-779dacfe00fe','d79ab826-65de-4fda-8b5f-779dacfe00fe',0);

-- INSERT INTO t_organization (id,"name",contact_id,type_code,status_code,creation_date,modification_date,created_by_id,modified_by_id,"version") VALUES
--                            ('df0b272f-dcfb-446c-81a5-ae70222d1e49','EVERY_SHOT_COUNTS_DEV','31fbe297-d72a-475c-bfd0-23f3277e88da','ACTIVE','ACTIVE',1692634291176,1692634291176,'d79ab826-65de-4fda-8b5f-779dacfe00fe','d79ab826-65de-4fda-8b5f-779dacfe00fe',0);

-- INSERT INTO t_team (id,sport_id,organization_id,"name",description) VALUES
--                    ('93c71afc-552c-44fc-99bb-c9ff95b24233','e75b593b-0af6-4c80-b709-38760c578db6','df0b272f-dcfb-446c-81a5-ae70222d1e49','EVERY_SHOT_COUNTS_DEV','Every Shot Counts Dev Team #1');

-- INSERT INTO t_team_athlete_xref VALUES ('93c71afc-552c-44fc-99bb-c9ff95b24233', 'ded998bc-e221-463b-b621-c10983555b4d', 'VERIFIED', 'FORWARD');
-- INSERT INTO t_team_athlete_xref VALUES ('93c71afc-552c-44fc-99bb-c9ff95b24233', 'fad2c32f-3952-42ae-ad0c-3f9feb703e6c', 'VERIFIED', 'FORWARD');
