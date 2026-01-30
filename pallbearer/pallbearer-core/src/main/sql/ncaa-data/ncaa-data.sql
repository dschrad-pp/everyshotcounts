--
-- Queries for creating staging tables and queries for generating Dart code
-- from the staging tables
--
-- Assumes datafiles are read from /tmp
--

--
-- NCAA Conference staging data
DROP TABLE IF EXISTS s_ncaa_data;

CREATE TABLE IF NOT EXISTS s_ncaa_data (
    team VARCHAR(120),
    conf VARCHAR(120),
    g VARCHAR(120),
    w VARCHAR(120),
    adjoe VARCHAR(120),
    adjde VARCHAR(120),
    barthag VARCHAR(120),
    efg_o VARCHAR(120),
    efg_d VARCHAR(120),
    tor VARCHAR(120),
    tord VARCHAR(120),
    orb VARCHAR(120),
    drb VARCHAR(120),
    ftr VARCHAR(120),
    ftrd VARCHAR(120),
    two_p_o VARCHAR(120),
    two_p_d VARCHAR(120),
    three_p_o VARCHAR(120),
    three_p_d VARCHAR(120),
    adj_t VARCHAR(120),
    wab VARCHAR(120),
    see VARCHAR(120),
    id UUID DEFAULT uuid_generate_v4() PRIMARY KEY
);

COPY s_ncaa_data(team,conf,g,w,adjoe,adjde,barthag,efg_o,efg_d,tor,
tord,orb,drb,ftr,ftrd,two_p_o,two_p_d,three_p_o,three_p_d,adj_t,wab,see)
FROM '/tmp/cbb21.csv' DELIMITER ',' CSV HEADER;

DROP TABLE IF EXISTS s_ncaa_conference;

CREATE TABLE IF NOT EXISTS s_ncaa_conference (
  id UUID DEFAULT uuid_generate_v4() PRIMARY KEY,
  name VARCHAR(256),
  alias VARCHAR(64)
);

COPY s_ncaa_conference(alias,name,id)
FROM '/tmp/ncaa-conferences.csv' DELIMITER ',' CSV HEADER;

--
-- NCAA Team staging data

DROP TABLE IF EXISTS s_ncaa_team;

CREATE TABLE IF NOT EXISTS s_ncaa_team (
  id UUID DEFAULT uuid_generate_v4() PRIMARY KEY,
  name VARCHAR(256),
  conference_alias VARCHAR(64)
);

COPY s_ncaa_team(id, name, conference_alias)
FROM '/tmp/ncaa-teams.csv' DELIMITER ',' CSV HEADER;

--
-- NCAA State staging data

DROP TABLE IF EXISTS s_ncaa_state;

CREATE TABLE IF NOT EXISTS s_ncaa_state (
  id UUID DEFAULT uuid_generate_v4() PRIMARY KEY,
  name VARCHAR(256),
  alias VARCHAR(64),
  ncaa_participant boolean not null default true
);

COPY s_ncaa_state(id, name, alias, ncaa_participant)
FROM '/tmp/ncaa-states.csv' DELIMITER ',' CSV HEADER;

--
-- NCAA Conference Dart generation

-- select conferences as static class members. Example:
--   static Conference atlantic10 = Conference(id, name, alias);
with cte_values as (
  select id as id
       , alias as alias
       , name as name
       , replace(replace(name, ' ', ''), '-', '') as memberName
    from s_ncaa_conference
   order by name asc
) select 'static NcaaConference ' ||
        lower(substring(cv.memberName from 1 for 1)) ||
          substring(cv.memberName from 2 for length(cv.memberName))
          || ' = NcaaConference(' ||
         '"' || cv.id || '", '
         '"' || cv.name || '", '
         '"' || cv.alias || '");'
    from cte_values cv order by name asc;

-- Select conferences as a list of member names
with cte_values as (
  select id as id
       , alias as alias
       , name as name
       , replace(replace(name, ' ', ''), '-', '') as memberName
    from s_ncaa_conference
   order by name asc
) select lower(substring(cv.memberName from 1 for 1)) ||
          substring(cv.memberName from 2 for length(cv.memberName))
          || ','
    from cte_values cv order by name asc;

--
-- NCAA Team Dart generation

-- select teams as static class members. Example:
--   static NcaaTeam abileneChristian = NcaaConference(id, name, nickname, alias);
with cte_values as (
  select nt.id as id
       , nt.name as name
       , '' as nickname
       , regexp_replace(nt.name, '\W', '', 'g') as teamMemberName
       , replace(replace(nc.name, ' ',''), '-', '') as conferenceMemberName
   from s_ncaa_team nt
    join s_ncaa_conference nc on nc.alias = nt.conference_alias
  order by name asc
) select 'static NcaaTeam ' ||
         lower(substring(cv.teamMemberName from 1 for 1)) ||
           substring(cv.teamMemberName from 2 for length(cv.teamMemberName)) ||
         ' = NcaaTeam(' ||
         '"' || cv.id || '", ' ||
         '"' || cv.name || '", ' ||
         '"' || cv.nickname || '", ' ||
         'NcaaConferenceConstants.' ||
           lower(substring(cv.conferenceMemberName from 1 for 1)) ||
           substring(cv.conferenceMemberName from 2 for length(cv.conferenceMemberName)) ||
         ');'
  from cte_values cv order by name asc;

-- Select teams as a list of member names
with cte_values as (
  select nt.id as id
       , nt.name as name
       , regexp_replace(nt.name, '\W', '', 'g') as teamMemberName
   from s_ncaa_team nt
  order by name asc
) select lower(substring(cv.teamMemberName from 1 for 1)) ||
         substring(cv.teamMemberName from 2 for length(cv.teamMemberName))
         || ','
    from cte_values cv order by cv.name asc;

--
-- NCAA State Dart generation

-- select states as static class members. Example:
--  static NcaaState alabama = NcaaState(id, name, alias);
with cte_values as (
  select id as id
       , alias as alias
       , name as name
       , regexp_replace(name, '\W', '', 'g') as memberName
    from s_ncaa_state
   where ncaa_participant = true
   order by name asc
) select 'static NcaaState ' ||
        lower(substring(cv.memberName from 1 for 1)) ||
          substring(cv.memberName from 2 for length(cv.memberName))
          || ' = NcaaState(' ||
         '"' || cv.id || '", '
         '"' || cv.name || '", '
         '"' || cv.alias || '");'
    from cte_values cv order by name asc;

-- Select states as a list of member names
with cte_values as (
  select ns.id as id
       , ns.name as name
       , regexp_replace(ns.name, '\W', '', 'g') as memberName
   from s_ncaa_state ns
  where ncaa_participant = true
  order by name asc
) select lower(substring(cv.memberName from 1 for 1)) ||
         substring(cv.memberName from 2 for length(cv.memberName))
         || ','
    from cte_values cv order by cv.name asc;



