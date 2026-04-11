-- Create the team-user cross-reference table.
-- This table was always commented out in the initial schema but is referenced by
-- TeamBaseDao.insertUserTeamMapping() and CoachDao queries.
CREATE TABLE IF NOT EXISTS t_team_user_xref (
    team_id           UUID NOT NULL REFERENCES t_team(id) DEFERRABLE INITIALLY DEFERRED,
    user_id           UUID NOT NULL REFERENCES t_user(id) DEFERRABLE INITIALLY DEFERRED,
    verification_code VARCHAR(40) DEFAULT 'UNVERIFIED' NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_team_user_xref_team_id ON t_team_user_xref(team_id);
CREATE INDEX IF NOT EXISTS idx_team_user_xref_user_id ON t_team_user_xref(user_id);
