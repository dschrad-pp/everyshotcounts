
-- CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- CREATE EXTENSION IF NOT EXISTS "tablefunc";

-- DROP TABLE IF EXISTS t_contact CASCADE;

-- -- Notes: User contact data. The verification status code identifies that we believe the user is who they say they
-- -- are... For example: this is the real Lebron James
-- CREATE TABLE t_contact (
--     id                 UUID DEFAULT uuid_generate_v4() PRIMARY KEY,
--     contact_type       VARCHAR(40) NOT NULL,
--     first_name         VARCHAR(255) NOT NULL,
--     middle_name        VARCHAR(255),
--     last_name          VARCHAR(255) NOT NULL,
--     email              VARCHAR(1024) NOT NULL,
--     telephone          VARCHAR(1024) NOT NULL,
--     birth_date         BIGINT NOT NULL,
--     verification_code  VARCHAR(40) DEFAULT 'UNVERIFIED' NOT NULL,
--     creation_date      BIGINT NOT NULL DEFAULT (extract(epoch from now()) * 1000),
--     modification_date  BIGINT NOT NULL DEFAULT (extract(epoch from now()) * 1000),
--     created_by_id      UUID NOT NULL DEFAULT 'd79ab826-65de-4fda-8b5f-779dacfe00fe'::UUID,
--     modified_by_id     UUID NOT NULL DEFAULT 'd79ab826-65de-4fda-8b5f-779dacfe00fe'::UUID,
--     version            INTEGER NOT NULL DEFAULT 0
-- );


-- DROP TABLE IF EXISTS t_address CASCADE;

-- CREATE TABLE t_address (
--     id                 UUID DEFAULT uuid_generate_v4() PRIMARY KEY,
--     address_type       VARCHAR(40) NOT NULL, -- HOME, WORK, VACATION
--     -- adding here like this so we don't need PostGIS extensions from the start
--     geo_latitude       DECIMAL(10, 6),
--     geo_longitude      DECIMAL(10, 6),
--     address            TEXT,
--     city               TEXT,
--     state              VARCHAR(1024),
--     zip                VARCHAR(1024),
--     creation_date      BIGINT NOT NULL DEFAULT (extract(epoch from now()) * 1000),
--     modification_date  BIGINT NOT NULL DEFAULT (extract(epoch from now()) * 1000),
--     created_by_id      UUID NOT NULL DEFAULT 'd79ab826-65de-4fda-8b5f-779dacfe00fe'::UUID,
--     modified_by_id     UUID NOT NULL DEFAULT 'd79ab826-65de-4fda-8b5f-779dacfe00fe'::UUID,
--     version            INTEGER NOT NULL DEFAULT 0
-- );


-- DROP TABLE IF EXISTS t_organization CASCADE;

-- -- Notes: Provides a collection of teams.
-- CREATE TABLE t_organization (
--     id                 UUID DEFAULT uuid_generate_v4() PRIMARY KEY,
--     name               VARCHAR(255) NOT NULL,
--     contact_id         UUID NOT NULL REFERENCES t_contact(id) DEFERRABLE INITIALLY DEFERRED,
--     type_code          VARCHAR(40) DEFAULT 'NONE' NOT NULL,
--     status_code        VARCHAR(40) DEFAULT 'ACTIVE' NOT NULL,
--     creation_date      BIGINT NOT NULL DEFAULT (extract(epoch from now()) * 1000),
--     modification_date  BIGINT NOT NULL DEFAULT (extract(epoch from now()) * 1000),
--     created_by_id      UUID NOT NULL DEFAULT 'd79ab826-65de-4fda-8b5f-779dacfe00fe'::UUID,
--     modified_by_id     UUID NOT NULL DEFAULT 'd79ab826-65de-4fda-8b5f-779dacfe00fe'::UUID,
--     version            INTEGER NOT NULL DEFAULT 0
-- );


-- DROP TABLE IF EXISTS t_user CASCADE;

-- -- Notes: Basic user identification. If you can access the system, you are a user.
-- -- You are differentiated by which role you have
-- CREATE TABLE t_user (
--     id                 UUID DEFAULT uuid_generate_v4() PRIMARY KEY,
--     email              VARCHAR(255) NOT NULL,
--     user_alias         VARCHAR(255) NOT NULL,
--     username           VARCHAR(64) NOT NULL,
--     contact_id         UUID NOT NULL REFERENCES t_contact(id) DEFERRABLE INITIALLY DEFERRED,
--     keycloak_id        UUID,
--     avatar_byte_array  BYTEA,
--     avatar_mime_type   VARCHAR(512),
--     status_code        VARCHAR(40) DEFAULT 'ACTIVE' NOT NULL, -- needs to accept T&C to be registered
--     creation_date      BIGINT NOT NULL DEFAULT (extract(epoch from now()) * 1000),
--     modification_date  BIGINT NOT NULL DEFAULT (extract(epoch from now()) * 1000),
--     created_by_id      UUID NOT NULL DEFAULT 'd79ab826-65de-4fda-8b5f-779dacfe00fe'::UUID,
--     modified_by_id     UUID NOT NULL DEFAULT 'd79ab826-65de-4fda-8b5f-779dacfe00fe'::UUID,
--     version            INTEGER NOT NULL DEFAULT 0
-- );


-- DROP TABLE IF EXISTS t_user_property CASCADE;

-- CREATE TABLE t_user_property (
--     id                 UUID DEFAULT uuid_generate_v4() PRIMARY KEY,
--     user_id            UUID NOT NULL REFERENCES t_user(id) DEFERRABLE INITIALLY DEFERRED,
--     property_key       VARCHAR(1024) NOT NULL,
--     property_value     TEXT NOT NULL
-- );


-- DROP TABLE IF EXISTS t_role CASCADE;

-- CREATE TABLE t_role (
--     id                 UUID DEFAULT uuid_generate_v4() PRIMARY KEY,
--     name               VARCHAR(255),
--     description        VARCHAR(1024),
--     status_code        VARCHAR(40) DEFAULT 'ACTIVE' NOT NULL,
--     creation_date      BIGINT NOT NULL DEFAULT (extract(epoch from now()) * 1000),
--     modification_date  BIGINT NOT NULL DEFAULT (extract(epoch from now()) * 1000),
--     created_by_id      UUID NOT NULL DEFAULT 'd79ab826-65de-4fda-8b5f-779dacfe00fe'::UUID,
--     modified_by_id     UUID NOT NULL DEFAULT 'd79ab826-65de-4fda-8b5f-779dacfe00fe'::UUID,
--     version            INTEGER NOT NULL DEFAULT 0
-- );


-- DROP TABLE IF EXISTS t_user_role_xref CASCADE;

-- CREATE TABLE t_user_role_xref (
--     user_id            UUID NOT NULL REFERENCES t_user(id) DEFERRABLE INITIALLY DEFERRED,
--     role_id            UUID NOT NULL REFERENCES t_role(id) DEFERRABLE INITIALLY DEFERRED
-- );


-- DROP TABLE IF EXISTS t_notification CASCADE;

-- CREATE TABLE t_notification (
--     id                 UUID DEFAULT uuid_generate_v4() PRIMARY KEY,
--     name               VARCHAR(255),
--     description        VARCHAR(1024),
--     sent_user_id       UUID NOT NULL REFERENCES t_user(id) DEFERRABLE INITIALLY DEFERRED,
--     recd_user_id       UUID NOT NULL REFERENCES t_user(id) DEFERRABLE INITIALLY DEFERRED,
--     notification_type  VARCHAR(40) DEFAULT 'ACTIVE' NOT NULL,
--     status_code        VARCHAR(40) DEFAULT 'ACTIVE' NOT NULL,
--     creation_date      BIGINT NOT NULL DEFAULT (extract(epoch from now()) * 1000),
--     modification_date  BIGINT NOT NULL DEFAULT (extract(epoch from now()) * 1000),
--     created_by_id      UUID NOT NULL DEFAULT 'd79ab826-65de-4fda-8b5f-779dacfe00fe'::UUID,
--     modified_by_id     UUID NOT NULL DEFAULT 'd79ab826-65de-4fda-8b5f-779dacfe00fe'::UUID,
--     version            INTEGER NOT NULL DEFAULT 0
-- );


-- -- Notes: Identifies a media object. Bytes or URL may be null. For example, if the media object is an image
-- -- its content may live in the database. On the other hand a video may live at a URL in S3
-- DROP TABLE IF EXISTS t_media CASCADE;
-- CREATE TABLE t_media (
--     id                 UUID DEFAULT uuid_generate_v4() PRIMARY KEY,
--     name               VARCHAR(2048),
--     description        TEXT,
--     byte_array         BYTEA,
--     content_url        VARCHAR(2048),
--     status_code        VARCHAR(40) DEFAULT 'ACTIVE' NOT NULL,
--     mime_type          VARCHAR(255),
--     creation_date      BIGINT NOT NULL DEFAULT (extract(epoch from now()) * 1000),
--     modification_date  BIGINT NOT NULL DEFAULT (extract(epoch from now()) * 1000),
--     created_by_id      UUID NOT NULL DEFAULT 'd79ab826-65de-4fda-8b5f-779dacfe00fe'::UUID,
--     modified_by_id     UUID NOT NULL DEFAULT 'd79ab826-65de-4fda-8b5f-779dacfe00fe'::UUID,
--     version            INTEGER NOT NULL DEFAULT 0
-- );


-- DROP TABLE IF EXISTS t_sport CASCADE;

-- -- Notes: We want to organize the challenges by sport, team, etc so we can search, sort, group by sport
-- CREATE TABLE t_sport (
--     id                 UUID DEFAULT uuid_generate_v4() PRIMARY KEY,
--     name               VARCHAR(2048),
--     sport_code         VARCHAR(120)
-- );


-- DROP TABLE IF EXISTS t_team CASCADE;

-- -- Notes: This is intentionally not modeling the team / sport relationship as many to many. The Oregon Ducks are a team,
-- -- but we are going to want to be more specific: Oregon Ducks Men’s Basketball. This way it is easy to identify teams
-- -- and team members when creating challenges.
-- CREATE TABLE t_team (
--     id                 UUID DEFAULT uuid_generate_v4() PRIMARY KEY,
--     sport_id           UUID NOT NULL REFERENCES t_sport(id) DEFERRABLE INITIALLY DEFERRED,
--     organization_id    UUID NOT NULL REFERENCES t_organization(id) DEFERRABLE INITIALLY DEFERRED,
--     name               VARCHAR(2048),
--     description        TEXT
-- );


-- DROP TABLE IF EXISTS t_team_user_xref CASCADE;

-- -- Notes: Cross reference between team and user. Users can belong to multiple teams. We’ll want the ability to invite a
-- -- team to a challenge; this is a good way of identifying users as a group. If a user belongs to a college or PRO team,
-- -- their membership is verified
-- CREATE TABLE t_team_user_xref (
--     team_id            UUID NOT NULL REFERENCES t_team(id) DEFERRABLE INITIALLY DEFERRED,
--     user_id            UUID NOT NULL REFERENCES t_user(id) DEFERRABLE INITIALLY DEFERRED,
--     verification_code  VARCHAR(40) DEFAULT 'UNVERIFIED' NOT NULL
-- );


-- DROP TABLE IF EXISTS t_sport_challenge_type CASCADE;

-- -- Notes: Initially we are going to control the type of challenge that is available for each sport.
-- --   Basketball: FREE_THROW, THREE_POINT, etc;
-- --   Golf: PUTT, CHIP_SHOT, etc
-- CREATE TABLE t_sport_challenge_type (
--     id                   UUID DEFAULT uuid_generate_v4() PRIMARY KEY,
--     sport_id             UUID NOT NULL REFERENCES t_sport(id) DEFERRABLE INITIALLY DEFERRED,
--     sport_challenge_code VARCHAR(40), -- FREE_THROW, THREE_POINT, PUTT, CHIP_SHOT
--     name                 VARCHAR(2048),
--     description          TEXT,
--     creation_date        BIGINT NOT NULL DEFAULT (extract(epoch from now()) * 1000),
--     modification_date    BIGINT NOT NULL DEFAULT (extract(epoch from now()) * 1000),
--     created_by_id        UUID NOT NULL DEFAULT 'd79ab826-65de-4fda-8b5f-779dacfe00fe'::UUID,
--     modified_by_id       UUID NOT NULL DEFAULT 'd79ab826-65de-4fda-8b5f-779dacfe00fe'::UUID,
--     version              INTEGER NOT NULL DEFAULT 0
-- );


-- DROP TABLE IF EXISTS t_challenge CASCADE;

-- -- Notes: When a challenge is created it has a start time and an end (or expiration) time. We’ll want to know who
-- -- created the challenge. Challenges can be of different types and I am suggesting that a default type of CHALLENGE
-- -- is between 2 people. A COMPETITION is probably organized by an external party and involves multiple people or teams.
-- -- Open Invite allows friends to invite their friends. Challenges have an execution time limit. The default is 5
-- -- minutes.
-- CREATE TABLE t_challenge (
--     id                        UUID DEFAULT uuid_generate_v4() PRIMARY KEY,
--     name                      VARCHAR(2048),
--     description               TEXT,
--     start_time                BIGINT NOT NULL DEFAULT (extract(epoch from now()) * 1000),
--     end_time                  BIGINT NOT NULL DEFAULT (extract(epoch from now()) * 1000),
--     sport_challenge_type_id   UUID NOT NULL REFERENCES t_sport_challenge_type(id) DEFERRABLE INITIALLY DEFERRED,
--     challenge_type_code       VARCHAR(40) DEFAULT 'CHALLENGE' NOT NULL, -- CHALLENGE, COMPETITION
--     challenge_visibility_code VARCHAR(40) DEFAULT 'PRIVATE' NOT NULL, -- PUBLIC,  PRIVATE
--     allow_retry_code          VARCHAR(40) DEFAULT 'ACTIVE' NOT NULL,
--     retry_max                 INTEGER NOT NULL DEFAULT 3,
--     allow_open_invite_code    VARCHAR(40) DEFAULT 'CLOSED' NOT NULL, -- OPEN, CLOSED
--     time_limit_ms             BIGINT NOT NULL DEFAULT 300000,
--     creation_date             BIGINT NOT NULL DEFAULT (extract(epoch from now()) * 1000),
--     modification_date         BIGINT NOT NULL DEFAULT (extract(epoch from now()) * 1000),
--     created_by_id             UUID NOT NULL DEFAULT 'd79ab826-65de-4fda-8b5f-779dacfe00fe'::UUID,
--     modified_by_id            UUID NOT NULL DEFAULT 'd79ab826-65de-4fda-8b5f-779dacfe00fe'::UUID,
--     version                   INTEGER NOT NULL DEFAULT 0
-- );


-- DROP TABLE IF EXISTS t_challenge_token CASCADE;

-- -- Notes: A user needs a token to participate in, and submit an entry to, a challenge. It may be possible for users
-- -- to have multiple challenge tokens (for example: purchasing retry tokens). When a user accepts a challenge
-- -- they get 1 or more tokens
-- CREATE TABLE t_challenge_token (
--     id                 UUID DEFAULT uuid_generate_v4() PRIMARY KEY,
--     challenge_id       UUID NOT NULL REFERENCES t_challenge(id) DEFERRABLE INITIALLY DEFERRED,
--     user_id            UUID NOT NULL REFERENCES t_user(id) DEFERRABLE INITIALLY DEFERRED,
--     token_status_code  VARCHAR(40) DEFAULT 'ACTIVE' NOT NULL, -- ACTIVE, CONSUMED
--     token_type_code    VARCHAR(40) DEFAULT 'INITIAL' NOT NULL -- INITIAL, RETRY
-- );


-- DROP TABLE IF EXISTS t_challenge_invitation CASCADE;

-- -- Notes: Allows a user to invite another user to a challenge. When a user accepts the invite they become a challenge
-- -- participant.
-- CREATE TABLE t_challenge_invitation (
--     id                 UUID DEFAULT uuid_generate_v4() PRIMARY KEY,
--     challenge_id       UUID NOT NULL REFERENCES t_challenge(id) DEFERRABLE INITIALLY DEFERRED,
--     invite_user_id     UUID NOT NULL REFERENCES t_user(id) DEFERRABLE INITIALLY DEFERRED,
--     token_id           UUID NOT NULL REFERENCES t_challenge_token(id) DEFERRABLE INITIALLY DEFERRED,
--     creation_date      BIGINT NOT NULL DEFAULT (extract(epoch from now()) * 1000),
--     modification_date  BIGINT NOT NULL DEFAULT (extract(epoch from now()) * 1000),
--     created_by_id      UUID NOT NULL DEFAULT 'd79ab826-65de-4fda-8b5f-779dacfe00fe'::UUID,
--     modified_by_id     UUID NOT NULL DEFAULT 'd79ab826-65de-4fda-8b5f-779dacfe00fe'::UUID,
--     version            INTEGER NOT NULL DEFAULT 0

-- );


-- DROP TABLE IF EXISTS t_challenge_participant CASCADE;

-- -- Notes: Identifies users participating in the challenge. When a user accepts an invite they become participants.
-- -- It may be possible that users are participating in the challenge with some role other than as a competitor,
-- -- for example, they are just there to watch and vote
-- CREATE TABLE t_challenge_participant (
--     id                    UUID DEFAULT uuid_generate_v4() PRIMARY KEY,
--     challenge_id          UUID NOT NULL REFERENCES t_challenge(id) DEFERRABLE INITIALLY DEFERRED,
--     participant_user_id   UUID NOT NULL REFERENCES t_user(id) DEFERRABLE INITIALLY DEFERRED,
--     participant_role_code VARCHAR(40) DEFAULT 'COMPETITOR' NOT NULL -- COMPETITOR, SPECTATOR
-- );


-- DROP TABLE IF EXISTS t_challenge_entry CASCADE;

-- -- Notes: Identifies a user's content submission to a challenge.
-- CREATE TABLE t_challenge_entry (
--     id                 UUID DEFAULT uuid_generate_v4() PRIMARY KEY,
--     challenge_id       UUID NOT NULL REFERENCES t_challenge(id) DEFERRABLE INITIALLY DEFERRED,
--     token_id           UUID NOT NULL REFERENCES t_challenge_token(id) DEFERRABLE INITIALLY DEFERRED,
--     media_id           UUID NOT NULL REFERENCES t_media(id) DEFERRABLE INITIALLY DEFERRED,
--     score              INTEGER NOT NULL DEFAULT 0,
--     creation_date      BIGINT NOT NULL DEFAULT (extract(epoch from now()) * 1000),
--     modification_date  BIGINT NOT NULL DEFAULT (extract(epoch from now()) * 1000),
--     created_by_id      UUID NOT NULL DEFAULT 'd79ab826-65de-4fda-8b5f-779dacfe00fe'::UUID,
--     modified_by_id     UUID NOT NULL DEFAULT 'd79ab826-65de-4fda-8b5f-779dacfe00fe'::UUID,
--     version            INTEGER NOT NULL DEFAULT 0
-- );


-- DROP TABLE IF EXISTS t_challenge_entry_vote CASCADE;

-- -- Notes: Users can vote yay or nay on challenge entries. A user does not have to be a participant in the
-- -- challenge to vote on it
-- CREATE TABLE t_challenge_entry_vote (
--     id                 UUID DEFAULT uuid_generate_v4() PRIMARY KEY,
--     challenge_entry_id UUID NOT NULL REFERENCES t_challenge_entry(id) DEFERRABLE INITIALLY DEFERRED,
--     vote_type_code     VARCHAR(40) DEFAULT 'YAY' NOT NULL, -- YAY, NAY
--     creation_date      BIGINT NOT NULL DEFAULT (extract(epoch from now()) * 1000),
--     modification_date  BIGINT NOT NULL DEFAULT (extract(epoch from now()) * 1000),
--     created_by_id      UUID NOT NULL DEFAULT 'd79ab826-65de-4fda-8b5f-779dacfe00fe'::UUID,
--     modified_by_id     UUID NOT NULL DEFAULT 'd79ab826-65de-4fda-8b5f-779dacfe00fe'::UUID,
--     version            INTEGER NOT NULL DEFAULT 0
-- );

