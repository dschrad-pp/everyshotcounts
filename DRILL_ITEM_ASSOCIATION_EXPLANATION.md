# How Drill Items are Associated with Athletes

## Overview

This document explains how drill items are associated with athletes/users in the API endpoint:
```
GET /api/drill/detail/athlete/{athleteUserId}/group/{drillGroupId}
```

## Database Schema

The association is managed through three main tables:

### 1. `t_drill_item` (Drill Item Template)
- Contains the **definition** of drill items (templates)
- Fields include: `id`, `drill_group_id`, `name`, `description`, `media_id`, `level_index`, etc.
- These are **templates** that all athletes can access

### 2. `t_drill` (Athlete Drill Submission/Association)
- This is the **junction/association table** that links athletes to drill items
- Key fields:
  - `id` - Primary key
  - `drill_item_id` - Foreign key to `t_drill_item.id`
  - `user_id` - Foreign key to `t_user.id` (the athlete)
  - `media_id` - Media submitted by the athlete for this drill
  - `drill_status` - Status of the drill (e.g., 'OPEN', 'COMPLETE', 'PROCESSING', etc.)
  - `attempts_detected`, `makes_detected`, `attempts_reported`, `makes_reported`
- **A row is created when an athlete attempts/submits a drill item**

### 3. `t_drill_group` (Drill Group)
- Groups drill items together (e.g., "Beginner", "Intermediate", "Advanced", "Elite")
- `t_drill_item.drill_group_id` references this table

## Association Logic

### SQL Query Analysis

The endpoint uses the SQL query `getByAthleteAndGroup()` (in `AthleteDrillDetailDao.sql.stg`):

```sql
FROM t_drill_item di
JOIN t_drill_group dg ON dg.id = di.drill_group_id
LEFT JOIN t_drill dr ON dr.drill_item_id = di.id
     AND dr.user_id = :athleteUserId
WHERE dg.id = :drillGroupId
ORDER BY di.order_index ASC, di.level_index ASC, di.drill_item_order ASC
```

### Key Points:

1. **LEFT JOIN with `t_drill`**: 
   - The query uses a `LEFT JOIN`, which means **all drill items** in the specified group are returned
   - If an athlete has attempted a drill item, the `t_drill` row data is included
   - If an athlete has **not** attempted a drill item, the drill item is still returned but with NULL values for drill submission data

2. **Association Condition**:
   ```sql
   LEFT JOIN t_drill dr ON dr.drill_item_id = di.id
        AND dr.user_id = :athleteUserId
   ```
   - This matches drill items to the athlete's drill submissions
   - The association is based on:
     - `dr.drill_item_id = di.id` (same drill item)
     - `dr.user_id = :athleteUserId` (same athlete)

3. **No Direct Association**:
   - Drill items are **NOT directly associated** with athletes
   - The association is **implicit** through the `t_drill` table
   - All athletes can see all drill items in a group
   - The `t_drill` table tracks which drill items an athlete has attempted/submitted

## Data Flow

### When an Athlete Views Drill Items:

1. **API Call**: `GET /api/drill/detail/athlete/{athleteUserId}/group/{drillGroupId}`
2. **Resource Handler**: `DrillDetailResource.getAthleteGroupDrillDetails()`
3. **Service Layer**: `AthleteDrillService.findWithAthleteAndGroup()`
4. **DAO Query**: `AthleteDrillDetailDao.getByAthleteAndGroup()`
5. **SQL Execution**: Returns all drill items in the group, with optional drill submission data if the athlete has attempted them

### When an Athlete Submits a Drill:

1. Athlete submits media for a drill item
2. A new row is created in `t_drill` table with:
   - `drill_item_id` = the drill item ID
   - `user_id` = the athlete's user ID
   - `media_id` = the submitted media
   - `drill_status` = 'PROCESSING' (initially)
3. This creates the association between the athlete and the drill item

## Response Structure

The API returns `List<AthleteDrillDetail>`, which contains:

- **Drill Item Properties** (from `t_drill_item`):
  - `drillItemId`, `name`, `description`, `mediaId`, `levelIndex`, etc.
  
- **Drill Group** (from `t_drill_group`):
  - `drillGroup` object with group information
  
- **Optional Drill Detail** (from `t_drill`):
  - `drillDetail` - Only present if the athlete has attempted this drill item
  - Contains: `id`, `drillStatus`, `mediaId`, `attemptsDetected`, `makesDetected`, etc.
  
- **Lock Status**:
  - `isLocked` - Determined by business logic (currently all drills are unlocked)

## Important Notes

1. **All Drill Items are Visible**: All drill items in a group are returned, regardless of whether the athlete has attempted them
2. **Association is Optional**: The `drillDetail` field is optional - it's only populated if the athlete has a corresponding row in `t_drill`
3. **One-to-Many Relationship**: An athlete can have multiple drill submissions for the same drill item (based on `retry_max` settings)
4. **No Pre-assignment**: Drill items are not pre-assigned to athletes - they become associated when the athlete attempts them

## Code References

- **API Endpoint**: `pallbearer-api/src/main/java/com/lektralabs/thrones/pallbearer/api/resource/DrillDetailResource.java` (lines 81-102)
- **Service**: `pallbearer-core/src/main/java/com/lektralabs/thrones/pallbearer/jdbi/service/AthleteDrillService.java` (lines 103-127)
- **DAO**: `pallbearer-core/src/main/java/com/lektralabs/thrones/pallbearer/jdbi/dao/AthleteDrillDetailDao.java` (lines 44-46)
- **SQL Query**: `pallbearer-core/src/main/resources/com/lektralabs/thrones/pallbearer/jdbi/dao/AthleteDrillDetailDao.sql.stg` (lines 82-159)
