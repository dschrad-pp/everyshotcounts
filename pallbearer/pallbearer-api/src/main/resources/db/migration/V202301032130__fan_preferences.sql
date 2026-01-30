-- DROP TABLE IF EXISTS t_team_user_xref CASCADE;

-- -- Notes: Cross reference between team and user. Users can belong to multiple teams. We’ll want the ability to invite a
-- -- team to a challenge; this is a good way of identifying users as a group. If a user belongs to a college or PRO team,
-- -- their membership is verified
-- CREATE TABLE t_team_athlete_xref (
--     team_id            UUID NOT NULL REFERENCES t_team(id) DEFERRABLE INITIALLY DEFERRED,
--     athlete_id         UUID NOT NULL REFERENCES t_user(id) DEFERRABLE INITIALLY DEFERRED,
--     verification_code  VARCHAR(40) DEFAULT 'UNVERIFIED' NOT NULL,
--     position_code      VARCHAR(40) -- GUARD
-- );

-- -- could be a fan of a team, or a position, or a position on a team, or a player, etc
-- CREATE TABLE t_fan_xref (
--     fan_id             UUID NOT NULL REFERENCES t_user(id) DEFERRABLE INITIALLY DEFERRED,
--     team_id            UUID REFERENCES t_team(id) DEFERRABLE INITIALLY DEFERRED,
--     athlete_id         UUID REFERENCES t_user(id) DEFERRABLE INITIALLY DEFERRED,
--     position_code      VARCHAR(40), -- GUARD,
--     strength           INTEGER NOT NULL DEFAULT 1
-- );

-- CREATE TABLE t_user_media_xref (
--     user_id            UUID NOT NULL REFERENCES t_user(id) DEFERRABLE INITIALLY DEFERRED,
--     media_id           UUID NOT NULL REFERENCES t_media(id) DEFERRABLE INITIALLY DEFERRED,
--     media_type_code    VARCHAR(40), -- AVATAR, GALLERY
--     PRIMARY KEY (user_id, media_id)
-- );
