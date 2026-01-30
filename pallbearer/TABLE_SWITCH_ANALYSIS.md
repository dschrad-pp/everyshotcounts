# Analysis: Switching from `t_drill_item` to `t_drill_item_import`

## Overview
This document identifies all places where `t_drill_item` is used and needs to be changed to `t_drill_item_import` for testing purposes, while ensuring API responses remain unchanged.

## Critical Consideration

⚠️ **Important**: The `t_drill` table has a foreign key `drill_item_id` that references `t_drill_item.id`. When switching to `t_drill_item_import`:
- The JOIN `dr.drill_item_id = di.id` will work **ONLY IF** the IDs in `t_drill_item_import` match the IDs in `t_drill_item`
- If IDs don't match, the LEFT JOIN will return NULL for drill submission data
- **Solution**: Ensure imported data uses the same IDs as `t_drill_item`, OR update the JOIN condition

## Files That Need Changes

### 1. **AthleteDrillDetailDao.sql.stg** ⭐ PRIMARY (For API endpoint)
**File**: `pallbearer-core/src/main/resources/com/lektralabs/thrones/pallbearer/jdbi/dao/AthleteDrillDetailDao.sql.stg`

**Queries to change:**
- ✅ `getByAthleteAndGroup()` (Line 148) - **MAIN ONE for `/api/drill/detail/athlete/{id}/group/{id}`**
- ✅ `getByAthleteAndDrillItem()` (Line 69)
- ✅ `getByAthleteGroupAndLevel()` (Line 387)
- ✅ `getByAthleteTimeline()` (Line 226)
- ✅ `getByTeamTimeline()` (Line 306)

**Change**: Replace `FROM t_drill_item di` with `FROM t_drill_item_import di`

**Impact**: This affects the main API endpoint the user mentioned.

---

### 2. **DrillItemDetailDao.sql.stg**
**File**: `pallbearer-core/src/main/resources/com/lektralabs/thrones/pallbearer/jdbi/dao/DrillItemDetailDao.sql.stg`

**Queries to change:**
- ✅ `findDrillItemByGroupId()` (Line 117) - Used by `/api/drill_item_detail/group/{groupId}`
- ✅ `getByDrillItemId()` (Line 55) - Used by drill item detail endpoints
- ✅ `getTeamDrillItemDetails()` (Line 180) - Used by `/api/drill_item_detail/team/{teamId}`

**Change**: Replace `FROM t_drill_item di` with `FROM t_drill_item_import di`

**Impact**: Affects drill item detail endpoints.

---

### 3. **DrillItemDao.sql.stg**
**File**: `pallbearer-core/src/main/resources/com/lektralabs/thrones/pallbearer/jdbi/dao/DrillItemDao.sql.stg`

**Queries to change:**
- ✅ `findAll()` (Line 71) - Returns all drill items
- ✅ `findByDrillGroupId()` (Line 99) - Returns drill items by group
- ⚠️ `updateMediaId()` (Line 8) - **SKIP** - This is an UPDATE, keep using `t_drill_item`
- ⚠️ `updatePassingScore()` (Line 105) - **SKIP** - This is an UPDATE, keep using `t_drill_item`
- ✅ `findMaxItemOrder()` (Line 27) - Used for ordering, can switch

**Change**: Replace `FROM t_drill_item di` with `FROM t_drill_item_import di` for SELECT queries only.

**Impact**: Affects drill item listing endpoints.

---

### 4. **DrillItemBaseDao.sql.stg** ⚠️ DO NOT CHANGE
**File**: `pallbearer-core/src/main/resources/com/lektralabs/thrones/pallbearer/jdbi/dao/generated/DrillItemBaseDao.sql.stg`

**Reason**: This is generated code and used for CRUD operations (INSERT, UPDATE, DELETE). These should continue using `t_drill_item` for data persistence.

---

## Summary of Changes Needed

### High Priority (Affects Main API Endpoint)
1. ✅ **AthleteDrillDetailDao.sql.stg** - `getByAthleteAndGroup()` query
   - This is the query used by `/api/drill/detail/athlete/{athleteUserId}/group/{drillGroupId}`

### Medium Priority (Affects Other Endpoints)
2. ✅ **AthleteDrillDetailDao.sql.stg** - All other queries in this file
3. ✅ **DrillItemDetailDao.sql.stg** - All SELECT queries
4. ✅ **DrillItemDao.sql.stg** - SELECT queries only (not UPDATE queries)

### Low Priority / Skip
5. ⚠️ **DrillItemBaseDao.sql.stg** - Skip (generated code, used for writes)
6. ⚠️ **Update queries** - Skip (should continue using `t_drill_item`)

## Implementation Strategy

### Option 1: Direct Table Replacement (Recommended for Testing)
Replace `t_drill_item` with `t_drill_item_import` in SELECT queries only.

**Pros:**
- Simple change
- Easy to revert
- Good for testing

**Cons:**
- Need to ensure IDs match between tables for JOINs to work

### Option 2: UNION Query (More Complex)
Use UNION to query both tables.

**Pros:**
- Can test import table while keeping production data
- No ID matching required

**Cons:**
- More complex queries
- Potential for duplicates

## Files to Modify

1. `pallbearer-core/src/main/resources/com/lektralabs/thrones/pallbearer/jdbi/dao/AthleteDrillDetailDao.sql.stg`
   - Change: `FROM t_drill_item di` → `FROM t_drill_item_import di` (5 queries)

2. `pallbearer-core/src/main/resources/com/lektralabs/thrones/pallbearer/jdbi/dao/DrillItemDetailDao.sql.stg`
   - Change: `FROM t_drill_item di` → `FROM t_drill_item_import di` (3 queries)

3. `pallbearer-core/src/main/resources/com/lektralabs/thrones/pallbearer/jdbi/dao/DrillItemDao.sql.stg`
   - Change: `FROM t_drill_item di` → `FROM t_drill_item_import di` (3 SELECT queries only)
   - Keep: UPDATE queries unchanged

## Testing Checklist

After making changes, verify:
- [ ] API endpoint `/api/drill/detail/athlete/{id}/group/{id}` returns data
- [ ] Response structure matches original (same fields, same format)
- [ ] Drill submission data (from `t_drill` table) still joins correctly
- [ ] No errors in application logs
- [ ] Other drill item endpoints still work

## Reverting Changes

To revert back to `t_drill_item`:
- Simply change `t_drill_item_import` back to `t_drill_item` in the SQL files
- No code changes needed
- No database changes needed
