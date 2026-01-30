-- Create table for CRM registration data
CREATE TABLE IF NOT EXISTS t_crm_registration (
    registration_id               BIGINT NOT NULL CONSTRAINT t_crm_registration__registration_id__unique UNIQUE,
    user_id                       BIGINT,
    username                      TEXT,
    email                         TEXT,
    first_name                    TEXT,
    last_name                     TEXT,
    phone_number                  TEXT,
    role                          TEXT,
    team_id                       BIGINT,
    payment_status                TEXT,
    subscription_start_date       BIGINT,
    subscription_end_date         BIGINT,
    last_updated                  BIGINT NOT NULL,
    created_at                    TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at                    TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (registration_id)
);

CREATE INDEX IF NOT EXISTS idx_crm_registration_user_id ON t_crm_registration(user_id);
CREATE INDEX IF NOT EXISTS idx_crm_registration_email ON t_crm_registration(email);
CREATE INDEX IF NOT EXISTS idx_crm_registration_username ON t_crm_registration(username);
CREATE INDEX IF NOT EXISTS idx_crm_registration_last_updated ON t_crm_registration(last_updated);
