CREATE TABLE t_team_join_code (
    team_id   UUID       NOT NULL PRIMARY KEY REFERENCES t_team(id),
    join_code VARCHAR(9) NOT NULL UNIQUE
);
