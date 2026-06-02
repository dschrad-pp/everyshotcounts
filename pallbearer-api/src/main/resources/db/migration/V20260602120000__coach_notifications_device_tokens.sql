CREATE TABLE t_device_token (
    id          UUID DEFAULT uuid_generate_v4() PRIMARY KEY,
    user_id     UUID NOT NULL REFERENCES t_user(id),
    token       VARCHAR(512) NOT NULL,
    platform    VARCHAR(20) NOT NULL DEFAULT 'ios',
    created_at  BIGINT NOT NULL,
    updated_at  BIGINT NOT NULL,
    UNIQUE (user_id, platform)
);

CREATE TABLE t_coach_notification (
    id                 UUID DEFAULT uuid_generate_v4() PRIMARY KEY,
    coach_id           UUID NOT NULL REFERENCES t_user(id),
    athlete_id         UUID NOT NULL REFERENCES t_user(id),
    drill_id           UUID NOT NULL,
    drill_item_id      UUID NOT NULL,
    drill_name         VARCHAR(255) NOT NULL DEFAULT '',
    athlete_first_name VARCHAR(255) NOT NULL DEFAULT '',
    athlete_last_name  VARCHAR(255) NOT NULL DEFAULT '',
    completed_at       BIGINT NOT NULL,
    is_read            BOOLEAN NOT NULL DEFAULT FALSE,
    is_dismissed       BOOLEAN NOT NULL DEFAULT FALSE,
    creation_date      BIGINT NOT NULL
);

CREATE INDEX idx_coach_notification_coach_id ON t_coach_notification(coach_id);
CREATE INDEX idx_coach_notification_completed_at ON t_coach_notification(completed_at DESC);
