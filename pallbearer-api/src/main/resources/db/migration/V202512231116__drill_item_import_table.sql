-- Create replica table for drill_item import
CREATE TABLE IF NOT EXISTS t_drill_item_import (
    id                        UUID DEFAULT uuid_generate_v4() PRIMARY KEY,
    drill_group_id            UUID NOT NULL REFERENCES t_drill_group(id) DEFERRABLE INITIALLY DEFERRED,
    name                      VARCHAR(2048),
    description               TEXT,
    media_id                  UUID REFERENCES t_media(id) DEFERRABLE INITIALLY DEFERRED,
    media_thumbnail           VARCHAR(2048),
    level_index               INTEGER NOT NULL DEFAULT 1,
    drill_item_order          INTEGER NOT NULL DEFAULT 1,
    order_index               INTEGER,
    passing_score             INTEGER NOT NULL DEFAULT 3,
    visibility_code           VARCHAR(40) DEFAULT 'PRIVATE' NOT NULL,
    allow_retry_code          VARCHAR(40) DEFAULT 'ACTIVE' NOT NULL,
    retry_max                 INTEGER NOT NULL DEFAULT 3,
    time_limit_ms             BIGINT NOT NULL DEFAULT 300000,
    creation_date             BIGINT NOT NULL DEFAULT (extract(epoch from now()) * 1000),
    modification_date         BIGINT NOT NULL DEFAULT (extract(epoch from now()) * 1000),
    created_by_id             UUID NOT NULL DEFAULT 'd79ab826-65de-4fda-8b5f-779dacfe00fe'::UUID,
    modified_by_id            UUID NOT NULL DEFAULT 'd79ab826-65de-4fda-8b5f-779dacfe00fe'::UUID,
    version                   INTEGER NOT NULL DEFAULT 0,
    level_test                BOOLEAN NOT NULL DEFAULT FALSE,
    team_id                   UUID REFERENCES t_team(id) DEFERRABLE INITIALLY DEFERRED,
    shots_max                 INTEGER NOT NULL DEFAULT 20
);

