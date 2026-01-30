-- Create replica table for media import
CREATE TABLE IF NOT EXISTS t_media_import (
    id                        UUID DEFAULT uuid_generate_v4() PRIMARY KEY,
    name                      VARCHAR(2048),
    description               TEXT,
    content_url               VARCHAR(2048),
    status_code               VARCHAR(40) DEFAULT 'ACTIVE' NOT NULL,
    mime_type                 VARCHAR(255),
    creation_date             BIGINT NOT NULL DEFAULT (extract(epoch from now()) * 1000),
    modification_date         BIGINT NOT NULL DEFAULT (extract(epoch from now()) * 1000),
    created_by_id             UUID NOT NULL DEFAULT 'd79ab826-65de-4fda-8b5f-779dacfe00fe'::UUID,
    modified_by_id            UUID NOT NULL DEFAULT 'd79ab826-65de-4fda-8b5f-779dacfe00fe'::UUID,
    version                   INTEGER NOT NULL DEFAULT 0
);
