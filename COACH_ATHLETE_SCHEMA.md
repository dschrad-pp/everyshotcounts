# Database Table Schemas for Coach and Athlete

## Overview
In this system, **Coach** and **Athlete** are not separate tables. They are **roles** assigned to users through the role-based system. All users (coaches and athletes) are stored in the `t_user` table and differentiated by their roles in `t_role` and `t_user_role_xref` tables.

---

## Core Tables

### 1. `t_user`
**Purpose**: Stores all users in the system (both coaches and athletes)

```sql
CREATE TABLE t_user (
    id                 UUID DEFAULT uuid_generate_v4() PRIMARY KEY,
    email              VARCHAR(255) NOT NULL,
    user_alias         VARCHAR(255) NOT NULL,
    username           VARCHAR(64) NOT NULL,
    contact_id         UUID NOT NULL REFERENCES t_contact(id) DEFERRABLE INITIALLY DEFERRED,
    keycloak_id        UUID,
    avatar_byte_array  BYTEA,
    avatar_mime_type   VARCHAR(512),
    status_code        VARCHAR(40) DEFAULT 'ACTIVE' NOT NULL,
    creation_date      BIGINT NOT NULL DEFAULT (extract(epoch from now()) * 1000),
    modification_date  BIGINT NOT NULL DEFAULT (extract(epoch from now()) * 1000),
    created_by_id      UUID NOT NULL DEFAULT 'd79ab826-65de-4fda-8b5f-779dacfe00fe'::UUID,
    modified_by_id     UUID NOT NULL DEFAULT 'd79ab826-65de-4fda-8b5f-779dacfe00fe'::UUID,
    version            INTEGER NOT NULL DEFAULT 0
);
```

**Key Fields**:
- `id`: Unique identifier for the user
- `email`: User's email address
- `username`: Unique username
- `contact_id`: Foreign key to `t_contact` table
- `keycloak_id`: Integration with Keycloak authentication
- `avatar_byte_array`: User's avatar image (binary)
- `avatar_mime_type`: MIME type of avatar image

---

### 2. `t_contact`
**Purpose**: Stores contact information for users (coaches and athletes)

```sql
CREATE TABLE t_contact (
    id                 UUID DEFAULT uuid_generate_v4() PRIMARY KEY,
    contact_type       VARCHAR(40) NOT NULL,
    first_name         VARCHAR(255) NOT NULL,
    middle_name        VARCHAR(255),
    last_name          VARCHAR(255) NOT NULL,
    email              VARCHAR(1024) NOT NULL,
    telephone          VARCHAR(1024) NOT NULL,
    birth_date         BIGINT NOT NULL,
    verification_code  VARCHAR(40) DEFAULT 'UNVERIFIED' NOT NULL,
    creation_date      BIGINT NOT NULL DEFAULT (extract(epoch from now()) * 1000),
    modification_date  BIGINT NOT NULL DEFAULT (extract(epoch from now()) * 1000),
    created_by_id      UUID NOT NULL DEFAULT 'd79ab826-65de-4fda-8b5f-779dacfe00fe'::UUID,
    modified_by_id     UUID NOT NULL DEFAULT 'd79ab826-65de-4fda-8b5f-779dacfe00fe'::UUID,
    version            INTEGER NOT NULL DEFAULT 0
);
```

**Key Fields**:
- `id`: Unique identifier for the contact
- `contact_type`: Type of contact (e.g., 'USER')
- `first_name`, `last_name`: Person's name
- `email`: Contact email
- `telephone`: Contact phone number
- `birth_date`: Birth date (stored as BIGINT timestamp)
- `verification_code`: Verification status

---

### 3. `t_role`
**Purpose**: Defines available roles in the system (COACH, ATHLETE, etc.)

```sql
CREATE TABLE t_role (
    id                 UUID DEFAULT uuid_generate_v4() PRIMARY KEY,
    name               VARCHAR(255),
    description        VARCHAR(1024),
    status_code        VARCHAR(40) DEFAULT 'ACTIVE' NOT NULL,
    creation_date      BIGINT NOT NULL DEFAULT (extract(epoch from now()) * 1000),
    modification_date  BIGINT NOT NULL DEFAULT (extract(epoch from now()) * 1000),
    created_by_id      UUID NOT NULL DEFAULT 'd79ab826-65de-4fda-8b5f-779dacfe00fe'::UUID,
    modified_by_id     UUID NOT NULL DEFAULT 'd79ab826-65de-4fda-8b5f-779dacfe00fe'::UUID,
    version            INTEGER NOT NULL DEFAULT 0
);
```

**Key Fields**:
- `id`: Unique identifier for the role
- `name`: Role name (e.g., 'COACH', 'ATHLETE')
- `description`: Description of the role
- `status_code`: Status of the role (ACTIVE, etc.)

---

### 4. `t_user_role_xref`
**Purpose**: Links users to their roles (many-to-many relationship)

```sql
CREATE TABLE t_user_role_xref (
    user_id            UUID NOT NULL REFERENCES t_user(id) DEFERRABLE INITIALLY DEFERRED,
    role_id            UUID NOT NULL REFERENCES t_role(id) DEFERRABLE INITIALLY DEFERRED
);
```

**Key Fields**:
- `user_id`: Foreign key to `t_user`
- `role_id`: Foreign key to `t_role`

**Note**: A user can have multiple roles. To identify a coach, look for `role.name = 'COACH'`. To identify an athlete, look for `role.name = 'ATHLETE'`.

---

### 5. `t_user_property`
**Purpose**: Stores additional properties for users (key-value pairs)

```sql
CREATE TABLE t_user_property (
    id                 UUID DEFAULT uuid_generate_v4() PRIMARY KEY,
    user_id            UUID NOT NULL REFERENCES t_user(id) DEFERRABLE INITIALLY DEFERRED,
    property_key       VARCHAR(1024) NOT NULL,
    property_value     TEXT NOT NULL
);
```

**Key Fields**:
- `id`: Unique identifier
- `user_id`: Foreign key to `t_user`
- `property_key`: Property name
- `property_value`: Property value (stored as TEXT)

---

## Team and Organization Tables

### 6. `t_team`
**Purpose**: Stores team information

```sql
CREATE TABLE t_team (
    id                 UUID DEFAULT uuid_generate_v4() PRIMARY KEY,
    sport_id           UUID NOT NULL REFERENCES t_sport(id) DEFERRABLE INITIALLY DEFERRED,
    organization_id    UUID NOT NULL REFERENCES t_organization(id) DEFERRABLE INITIALLY DEFERRED,
    name               VARCHAR(2048),
    description        TEXT
);
```

**Key Fields**:
- `id`: Unique identifier for the team
- `sport_id`: Foreign key to `t_sport`
- `organization_id`: Foreign key to `t_organization`
- `name`: Team name
- `description`: Team description

---

### 7. `t_team_user_xref`
**Purpose**: Links users (coaches and athletes) to teams

```sql
CREATE TABLE t_team_user_xref (
    team_id            UUID NOT NULL REFERENCES t_team(id) DEFERRABLE INITIALLY DEFERRED,
    user_id            UUID NOT NULL REFERENCES t_user(id) DEFERRABLE INITIALLY DEFERRED,
    verification_code  VARCHAR(40) DEFAULT 'UNVERIFIED' NOT NULL
);
```

**Key Fields**:
- `team_id`: Foreign key to `t_team`
- `user_id`: Foreign key to `t_user`
- `verification_code`: Verification status (UNVERIFIED, VERIFIED, etc.)

**Note**: This table links both coaches and athletes to teams. The relationship between coach and athlete is established through shared team membership.

---

### 8. `t_organization`
**Purpose**: Stores organization information (collections of teams)

```sql
CREATE TABLE t_organization (
    id                 UUID DEFAULT uuid_generate_v4() PRIMARY KEY,
    name               VARCHAR(255) NOT NULL,
    contact_id         UUID NOT NULL REFERENCES t_contact(id) DEFERRABLE INITIALLY DEFERRED,
    type_code          VARCHAR(40) DEFAULT 'NONE' NOT NULL,
    status_code        VARCHAR(40) DEFAULT 'ACTIVE' NOT NULL,
    creation_date      BIGINT NOT NULL DEFAULT (extract(epoch from now()) * 1000),
    modification_date  BIGINT NOT NULL DEFAULT (extract(epoch from now()) * 1000),
    created_by_id      UUID NOT NULL DEFAULT 'd79ab826-65de-4fda-8b5f-779dacfe00fe'::UUID,
    modified_by_id     UUID NOT NULL DEFAULT 'd79ab826-65de-4fda-8b5f-779dacfe00fe'::UUID,
    version            INTEGER NOT NULL DEFAULT 0
);
```

**Key Fields**:
- `id`: Unique identifier for the organization
- `name`: Organization name
- `contact_id`: Foreign key to `t_contact`
- `type_code`: Type of organization
- `status_code`: Status (ACTIVE, etc.)

---

### 9. `t_organization_user_xref`
**Purpose**: Links users to organizations and teams

```sql
CREATE TABLE t_organization_user_xref (
    organization_id    UUID NOT NULL REFERENCES t_organization(id) DEFERRABLE INITIALLY DEFERRED,
    user_id            UUID NOT NULL REFERENCES t_user(id) DEFERRABLE INITIALLY DEFERRED,
    team_id            UUID NOT NULL REFERENCES t_team(id) DEFERRABLE INITIALLY DEFERRED,
    verification_code  VARCHAR(40) DEFAULT 'UNVERIFIED' NOT NULL
);
```

**Key Fields**:
- `organization_id`: Foreign key to `t_organization`
- `user_id`: Foreign key to `t_user`
- `team_id`: Foreign key to `t_team`
- `verification_code`: Verification status

---

## Additional Related Tables

### 10. `t_team_athlete_xref` (Alternative/Commented)
**Purpose**: Alternative table for linking athletes specifically to teams (with position)

```sql
CREATE TABLE t_team_athlete_xref (
    team_id            UUID NOT NULL REFERENCES t_team(id) DEFERRABLE INITIALLY DEFERRED,
    athlete_id         UUID NOT NULL REFERENCES t_user(id) DEFERRABLE INITIALLY DEFERRED,
    verification_code  VARCHAR(40) DEFAULT 'UNVERIFIED' NOT NULL,
    position_code      VARCHAR(40) -- GUARD, etc.
);
```

**Note**: This table appears to be commented out in the migrations. The system primarily uses `t_team_user_xref` for both coaches and athletes.

---

### 11. `t_user_media_xref`
**Purpose**: Links users to media objects

```sql
CREATE TABLE t_user_media_xref (
    user_id            UUID NOT NULL REFERENCES t_user(id) DEFERRABLE INITIALLY DEFERRED,
    media_id           UUID NOT NULL REFERENCES t_media(id) DEFERRABLE INITIALLY DEFERRED,
    media_type_code    VARCHAR(40), -- AVATAR, GALLERY
    PRIMARY KEY (user_id, media_id)
);
```

**Key Fields**:
- `user_id`: Foreign key to `t_user`
- `media_id`: Foreign key to `t_media`
- `media_type_code`: Type of media (AVATAR, GALLERY, etc.)

---

## How to Query Coaches and Athletes

### Find all Coaches:
```sql
SELECT u.*, c.first_name, c.last_name, r.name AS role_name
FROM t_user u
JOIN t_contact c ON u.contact_id = c.id
JOIN t_user_role_xref ur ON u.id = ur.user_id
JOIN t_role r ON ur.role_id = r.id
WHERE r.name = 'COACH';
```

### Find all Athletes:
```sql
SELECT u.*, c.first_name, c.last_name, r.name AS role_name
FROM t_user u
JOIN t_contact c ON u.contact_id = c.id
JOIN t_user_role_xref ur ON u.id = ur.user_id
JOIN t_role r ON ur.role_id = r.id
WHERE r.name = 'ATHLETE';
```

### Find all Athletes assigned to a Coach:
```sql
SELECT DISTINCT
    au.id                  AS athlete_user_id,
    au.email               AS athlete_email,
    au.username            AS athlete_username,
    ac.first_name          AS athlete_first_name,
    ac.last_name           AS athlete_last_name
FROM t_user au
JOIN t_contact ac ON au.contact_id = ac.id
JOIN t_user_role_xref aur ON au.id = aur.user_id
JOIN t_role ar ON aur.role_id = ar.id
JOIN t_team_user_xref atux ON au.id = atux.user_id
JOIN t_team t ON atux.team_id = t.id
JOIN t_team_user_xref ctux ON t.id = ctux.team_id
JOIN t_user cu ON ctux.user_id = cu.id
JOIN t_user_role_xref cur ON cu.id = cur.user_id
JOIN t_role cr ON cur.role_id = cr.id
WHERE ar.name = 'ATHLETE'
  AND cr.name = 'COACH'
  AND cu.id = :coachId;
```

---

## How Coaches and Athletes Are Connected

### Connection Mechanism

**Coaches and Athletes are connected through shared team membership**. They are not directly linked to each other, but rather both belong to the same team(s).

### The Connection Path

```
Coach (t_user) 
    ↓
t_user_role_xref (role = 'COACH')
    ↓
t_team_user_xref (coach linked to team)
    ↓
t_team (shared team)
    ↓
t_team_user_xref (athlete linked to same team)
    ↓
t_user_role_xref (role = 'ATHLETE')
    ↓
Athlete (t_user)
```

### Step-by-Step Explanation

1. **Both are Users**: Both coaches and athletes are stored in the `t_user` table
2. **Role Assignment**: Their roles are defined in `t_role` (COACH or ATHLETE) and linked via `t_user_role_xref`
3. **Team Membership**: Both coaches and athletes are linked to teams through `t_team_user_xref`
4. **Shared Teams**: When a coach and athlete belong to the same team (same `team_id` in `t_team_user_xref`), they are connected

### Visual Relationship Diagram

```
┌─────────────┐
│  t_user     │  (Coach)
│  (coach)    │
└──────┬──────┘
       │
       │ t_user_role_xref (role = 'COACH')
       │
       ▼
┌──────────────────┐
│ t_team_user_xref │  (coach_user_id, team_id)
└────────┬─────────┘
         │
         │ team_id
         ▼
    ┌─────────┐
    │ t_team  │  (Shared Team)
    └────┬────┘
         │
         │ team_id
         ▼
┌──────────────────┐
│ t_team_user_xref │  (athlete_user_id, team_id)
└────────┬─────────┘
         │
         │ t_user_role_xref (role = 'ATHLETE')
         │
         ▼
┌─────────────┐
│  t_user     │  (Athlete)
│  (athlete)  │
└─────────────┘
```

### Key Points

1. **Many-to-Many Relationship**: 
   - A coach can be connected to multiple athletes (through multiple teams)
   - An athlete can be connected to multiple coaches (if they belong to multiple teams with different coaches)

2. **Team-Based**: The relationship is always through teams. There is no direct coach-athlete link.

3. **Query Pattern**: To find all athletes assigned to a coach, the system:
   - Finds the coach's user record
   - Finds all teams the coach belongs to
   - Finds all athletes who belong to those same teams
   - Filters to ensure the users have the correct roles (COACH and ATHLETE)

### Example Scenario

**Scenario**: Coach "John Smith" coaches "Basketball Team A"

1. **Coach Setup**:
   - User record in `t_user` (id: `coach-uuid-123`)
   - Role link in `t_user_role_xref` (user_id: `coach-uuid-123`, role_id: `COACH-role-id`)
   - Team link in `t_team_user_xref` (user_id: `coach-uuid-123`, team_id: `team-a-uuid`)

2. **Athlete Setup**:
   - User record in `t_user` (id: `athlete-uuid-456`)
   - Role link in `t_user_role_xref` (user_id: `athlete-uuid-456`, role_id: `ATHLETE-role-id`)
   - Team link in `t_team_user_xref` (user_id: `athlete-uuid-456`, team_id: `team-a-uuid`)

3. **Connection**: 
   - Both share `team_id = team-a-uuid` in `t_team_user_xref`
   - This makes them connected - the coach can see and manage this athlete

### API Endpoint

The system provides an API endpoint to retrieve all athletes assigned to a coach:

```
GET /api/coach/{coachId}/athletes
```

This endpoint uses the `selectAllAthletesAssignedToCoach` query which follows the connection path described above.

---

---

## LeagueApps Integration: Source of Coach-Athlete Connections

### Overview

**Yes, the coach-athlete connection can come from LeagueApps**, which is an external registration and team management system. LeagueApps provides the initial data that establishes team memberships, which in turn creates the coach-athlete relationships.

### How LeagueApps Data Flows

```
LeagueApps (External System)
    ↓
    ↓ (API Sync)
    ↓
t_league_apps_registration (Registration data with teamId, role, etc.)
t_league_apps_member (Member data)
    ↓
    ↓ (Processing)
    ↓
t_user (Users created/updated)
t_user_role_xref (Roles assigned: COACH or ATHLETE)
t_team_user_xref (Team memberships created)
    ↓
    ↓ (Result)
    ↓
Coach-Athlete Connection (via shared team membership)
```

### LeagueApps Tables

#### `t_league_apps_registration`
**Purpose**: Stores registration data synced from LeagueApps

**Key Fields**:
- `registration_id`: Unique registration ID from LeagueApps
- `team_id`: **Team ID from LeagueApps** (this is the key!)
- `user_id`: User ID from LeagueApps
- `role`: Role from LeagueApps (e.g., "COACH", "ATHLETE", "PLAYER")
- `user_type`: Type of user
- `email`, `first_name`, `last_name`: User information
- `program_name`, `season`: Program details
- `registration_status`, `payment_status`: Status information

#### `t_league_apps_member`
**Purpose**: Stores member data synced from LeagueApps

**Key Fields**:
- `id`: Member ID from LeagueApps
- `user_id`: User ID from LeagueApps
- `username`: Username from LeagueApps
- `email`, `first_name`, `last_name`: Member information
- `group_id`, `group_name`: Group/team information
- `type`: Member type
- `org_account_role`: Organization account role (may indicate COACH)

### How the Connection is Established

1. **Data Sync from LeagueApps**:
   - The system periodically syncs data from LeagueApps via `/api/league_apps_integration/integrate`
   - Registration data includes `teamId` which indicates which team a user belongs to
   - Member data includes role information

2. **User Creation**:
   - When new registrations are detected, users are created in `t_user`
   - Roles are assigned based on LeagueApps data (COACH or ATHLETE)
   - Users are linked to roles via `t_user_role_xref`

3. **Team Assignment**:
   - The `teamId` from LeagueApps registration data is used to assign users to teams
   - Users are linked to teams via `t_team_user_xref`
   - **This is where the coach-athlete connection happens**: if a coach and athlete both have the same `teamId` in their LeagueApps registration, they get assigned to the same team in the system

4. **Connection Established**:
   - Once both coach and athlete are assigned to the same team via `t_team_user_xref`, they are connected
   - The system can then query athletes for a coach by finding all users on the same teams

### Example Flow from LeagueApps

**Scenario**: LeagueApps has a registration where:
- Coach "John Smith" is registered with `teamId = 12345` and `role = "COACH"`
- Athlete "Sarah Johnson" is registered with `teamId = 12345` and `role = "ATHLETE"`

**Process**:
1. LeagueApps sync fetches registration data
2. System creates/updates users in `t_user`
3. System assigns roles: John → COACH, Sarah → ATHLETE
4. System maps both to the same team (based on `teamId = 12345`) in `t_team_user_xref`
5. **Result**: Coach and Athlete are now connected through shared team membership

### Important Notes

1. **LeagueApps is the Source**: The initial team assignments and roles often come from LeagueApps registration data
2. **Team ID Mapping**: The `teamId` from LeagueApps needs to be mapped to the internal `t_team.id` in the system
3. **Manual Override Possible**: Team memberships can also be created manually, not just from LeagueApps
4. **Role Determination**: The `role` field in LeagueApps registration helps determine if a user is a COACH or ATHLETE
5. **Sync Process**: The sync happens via `LeagueAppsIntegration.update()` and `LeagueAppsIntegration.process()`

### API Endpoints for LeagueApps Integration

- `GET /api/league_apps_integration/integrate` - Triggers sync from LeagueApps
- `POST /api/league_apps_integration/register` - Register a new member from LeagueApps
- `GET /api/league_apps_integration/all-users` - Get all users from LeagueApps

---

## Summary

**Key Points**:
1. **No separate Coach or Athlete tables** - they are roles assigned to users
2. **Core tables**: `t_user`, `t_contact`, `t_role`, `t_user_role_xref`
3. **Team relationships**: `t_team`, `t_team_user_xref`, `t_organization`, `t_organization_user_xref`
4. **Additional data**: `t_user_property` for key-value properties, `t_user_media_xref` for media
5. **Coach-Athlete relationship**: Established through **shared team membership** via `t_team_user_xref`
   - Both coach and athlete must belong to the same team(s)
   - The relationship is many-to-many (one coach can have many athletes, one athlete can have many coaches)
   - No direct link exists between coach and athlete tables
6. **LeagueApps Integration**: 
   - **Yes, the connection often originates from LeagueApps**
   - LeagueApps provides registration data with `teamId` and `role`
   - Users are created/updated from LeagueApps data
   - Team assignments are made based on LeagueApps `teamId`
   - Coach-athlete connections are established when both are assigned to the same team from LeagueApps data

