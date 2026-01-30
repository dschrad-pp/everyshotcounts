# Alternative Ways to Test Import Data with Existing APIs

## Overview
Instead of changing all SQL queries from `t_drill_item` to `t_drill_item_import`, here are alternative approaches to test the imported data.

---

## Option 1: Copy Data from Import Table to Main Table ⭐ RECOMMENDED

**Approach**: After importing CSV data to `t_drill_item_import`, copy/sync it to `t_drill_item` so existing APIs work immediately.

### Implementation

#### A. Create API Endpoint to Sync Data

Add a new endpoint to `DrillItemImportResource.java`:

```java
@POST
@Path("/sync-to-main")
@RolesAllowed({"ADMIN", "COACH"})
@Produces(MediaType.APPLICATION_JSON)
public Response syncToMainTable(@QueryParam("drillGroupId") UUID drillGroupId) {
    int syncedCount = drillItemImportService.syncToMainTable(drillGroupId);
    return Response.ok()
            .entity("{\"message\": \"Synced " + syncedCount + " records to main table\", \"count\": " + syncedCount + "}")
            .build();
}
```

#### B. Add Service Method

In `DrillItemImportService.java`:

```java
@Transactional
public int syncToMainTable(UUID drillGroupId) {
    return jdbiProvider.getJdbi().inTransaction(TransactionIsolationLevel.SERIALIZABLE, handle -> {
        // Copy data from import table to main table
        String sql = """
            INSERT INTO t_drill_item (
                id, drill_group_id, name, description, media_id, media_thumbnail,
                level_index, drill_item_order, order_index, passing_score,
                visibility_code, allow_retry_code, retry_max, time_limit_ms,
                creation_date, modification_date, created_by_id, modified_by_id,
                version, level_test, team_id, shots_max
            )
            SELECT 
                id, drill_group_id, name, description, 
                CASE 
                    WHEN media_id ~ '^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$' 
                    THEN media_id::uuid 
                    ELSE NULL 
                END as media_id,
                media_thumbnail,
                level_index, drill_item_order, order_index, passing_score,
                visibility_code, allow_retry_code, retry_max, time_limit_ms,
                creation_date, modification_date, created_by_id, modified_by_id,
                version, level_test, team_id, shots_max
            FROM t_drill_item_import
            WHERE drill_group_id = :drillGroupId
            ON CONFLICT (id) DO UPDATE SET
                name = EXCLUDED.name,
                description = EXCLUDED.description,
                media_thumbnail = EXCLUDED.media_thumbnail,
                level_index = EXCLUDED.level_index,
                drill_item_order = EXCLUDED.drill_item_order,
                order_index = EXCLUDED.order_index,
                passing_score = EXCLUDED.passing_score,
                visibility_code = EXCLUDED.visibility_code,
                allow_retry_code = EXCLUDED.allow_retry_code,
                retry_max = EXCLUDED.retry_max,
                time_limit_ms = EXCLUDED.time_limit_ms,
                modification_date = EXCLUDED.modification_date,
                modified_by_id = EXCLUDED.modified_by_id,
                version = EXCLUDED.version + 1,
                level_test = EXCLUDED.level_test,
                team_id = EXCLUDED.team_id,
                shots_max = EXCLUDED.shots_max
            """;
        
        return handle.createUpdate(sql)
                .bind("drillGroupId", drillGroupId)
                .execute();
    });
}
```

**Pros:**
- ✅ No changes to existing SQL queries
- ✅ Existing APIs work immediately
- ✅ Can test with real production-like setup
- ✅ Easy to revert (just delete synced data)

**Cons:**
- ⚠️ Modifies production table `t_drill_item`
- ⚠️ Need to handle media_id conversion (string to UUID)

---

## Option 2: Create Test-Specific API Endpoints

**Approach**: Create new API endpoints that query `t_drill_item_import` directly, keeping existing endpoints unchanged.

### Implementation

Create `DrillItemImportTestResource.java`:

```java
@Path("/api/drill_item_import_test")
public class DrillItemImportTestResource {
    
    @Inject
    DrillItemImportService drillItemImportService;
    
    @Inject
    AthleteDrillService athleteDrillService; // Reuse existing service logic
    
    @GET
    @Path("/athlete/{athleteUserId}/group/{drillGroupId}")
    @RolesAllowed({"ADMIN", "COACH", "ATHLETE"})
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAthleteGroupDrillDetailsFromImport(
            @PathParam("athleteUserId") UUID athleteUserId,
            @PathParam("drillGroupId") UUID drillGroupId) {
        // Create a custom DAO method that queries t_drill_item_import
        // Reuse the same response structure
        return Response.ok(/* custom query results */).build();
    }
}
```

**Pros:**
- ✅ Doesn't affect existing APIs
- ✅ Can test side-by-side
- ✅ Safe for production

**Cons:**
- ⚠️ Need to duplicate some query logic
- ⚠️ More code to maintain

---

## Option 3: Database View (Union Both Tables)

**Approach**: Create a database view that combines both tables, then query the view.

### Implementation

Create migration:
```sql
CREATE OR REPLACE VIEW v_drill_item_test AS
SELECT * FROM t_drill_item_import
UNION ALL
SELECT * FROM t_drill_item WHERE id NOT IN (SELECT id FROM t_drill_item_import);
```

Then change queries to use `v_drill_item_test` instead of `t_drill_item`.

**Pros:**
- ✅ Single point of change (the view)
- ✅ Can prioritize import table data

**Cons:**
- ⚠️ Still need to change SQL queries (but only table name)
- ⚠️ UNION can be slower
- ⚠️ Need to handle column type differences (media_id)

---

## Option 4: Feature Flag / Configuration

**Approach**: Use application configuration to switch between tables.

### Implementation

In `application.properties`:
```properties
drill.item.use.import.table=false
```

In SQL templates, use conditional logic (if supported) or create wrapper methods.

**Pros:**
- ✅ Can toggle without code changes
- ✅ Easy to enable/disable

**Cons:**
- ⚠️ Complex to implement with StringTemplate
- ⚠️ May require code changes anyway

---

## Option 5: Temporary Table Swap (Advanced)

**Approach**: Rename tables temporarily for testing.

### Implementation

```sql
-- Backup original
ALTER TABLE t_drill_item RENAME TO t_drill_item_backup;
ALTER TABLE t_drill_item_import RENAME TO t_drill_item;
-- Test...
-- Restore
ALTER TABLE t_drill_item RENAME TO t_drill_item_import;
ALTER TABLE t_drill_item_backup RENAME TO t_drill_item;
```

**Pros:**
- ✅ No code changes needed
- ✅ Quick to test

**Cons:**
- ⚠️ Very risky (downtime, data loss risk)
- ⚠️ Not recommended for production

---

## Recommended Approach: Option 1 (Sync to Main Table)

### Workflow:

1. **Import CSV** → `t_drill_item_import`
   ```
   POST /api/drill_item_import/import
   ```

2. **Review/Validate** imported data
   ```
   GET /api/drill_item_import/
   ```

3. **Sync to Main Table** (when ready to test)
   ```
   POST /api/drill_item_import/sync-to-main?drillGroupId={id}
   ```

4. **Test with Existing APIs**
   ```
   GET /api/drill/detail/athlete/{id}/group/{id}
   ```

5. **Cleanup** (if needed)
   ```
   DELETE /api/drill_item_import/
   ```

### Benefits:
- ✅ No changes to existing SQL queries
- ✅ Existing APIs work immediately
- ✅ Can test incrementally (by drill group)
- ✅ Easy to rollback
- ✅ Production-safe

---

## Comparison Table

| Option | Code Changes | SQL Changes | Risk Level | Ease of Revert | Recommended |
|--------|-------------|-------------|------------|----------------|-------------|
| **Option 1: Sync** | Minimal | None | Low | Easy | ⭐⭐⭐⭐⭐ |
| **Option 2: Test Endpoints** | Moderate | None | Very Low | Easy | ⭐⭐⭐⭐ |
| **Option 3: View** | None | Table name only | Low | Easy | ⭐⭐⭐ |
| **Option 4: Feature Flag** | Complex | None | Low | Easy | ⭐⭐ |
| **Option 5: Table Swap** | None | None | Very High | Hard | ⭐ |

---

## Recommendation

**Use Option 1 (Sync to Main Table)** because:
1. Minimal code changes
2. No SQL query modifications needed
3. Existing APIs work immediately
4. Safe and reversible
5. Can test incrementally

Would you like me to implement Option 1?
