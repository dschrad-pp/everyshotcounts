# 06/07/2026

# Problem & current architecture: 

# Refactor: Stop bulk-creating placeholder drill rows on athlete login

## What's happening right now (in simple terms)

When an athlete logs in for the **first time** (via `/api/sso/crm-login` →
`UserService.activateUser()`), the backend runs `createDrillsForNewUser()`
(`UserService.java:691-717`). This method:

1. Loads **every drill that exists in the system** (`drillItemService.findAll()`,
   limit = `Integer.MAX_VALUE`)
2. Loops over all of them and inserts one row per drill into the `t_drill`
   table for that athlete, with status `NOT-ATTEMPTED` and all counters at zero

So the moment an athlete logs in, we write hundreds of "nothing happened yet"
rows into the database on their behalf — before they've touched a single drill.

## Why this is wrong

- **The table grows by multiplication, not by activity.** Row count becomes
  `(number of athletes) × (number of drills)`, instead of `(number of actual
  drill attempts)`. 100 athletes × ~300 drills = ~30,000 rows that represent
  *nothing happening*. 1,000 athletes = ~300,000 such rows. This is the
  "massive table" we hit the indexing error on.
- **It creates a write storm at the worst possible moment.** Login is exactly
  when you'd expect concurrent traffic (launches, marketing pushes, morning
  rush). A thousand athletes logging in at once means a thousand loops, each
  inserting hundreds of rows, all hitting `t_drill` simultaneously — heavy
  lock contention, since each insert runs in its own
  `SERIALIZABLE`-isolation transaction (`DrillService.create(partial,
  auditUserId)`, `DrillService.java:146`).
- **It's solving a problem the read path doesn't have.** The screen that
  lists an athlete's drills (`AthleteDrillService.findWithAthleteAndGroup()`,
  `AthleteDrillService.java:138-167`) **already** knows how to show
  "not attempted" drills without a database row — it builds an in-memory
  placeholder object (`id = null`, status `NOT_ATTEMPTED`, counts = 0) for any
  drill the athlete hasn't done yet. The bulk insert at login pre-materializes
  exactly the data this read path is already capable of synthesizing on the fly.
- **No safety net against duplicates.** `t_drill` has no unique constraint on
  `(user_id, drill_item_id)` (only `idx_drill_user_id` exists, added in
  `V20260606120000__performance_indexes.sql`). If activation retries or races,
  duplicate placeholder rows for the same athlete+drill can pile up silently
  (errors are swallowed at `UserService.java:714-715`).

**The industry-standard shape** is a static catalog table (`t_drill_item`:
title, level, difficulty — same for every athlete, read-mostly) plus a
*sparse* per-athlete progress table (`t_drill`) that only contains a row once
an athlete actually does something. "Not attempted" is represented by *absence*
of a row, not by a placeholder row. This codebase already implements that
pattern on the read side — it just isn't trusted on the write side.

## What the change should be

1. **Stop creating placeholder rows at login.**
   Remove the call to `createDrillsForNewUser(userRow.getId())` at
   `UserService.java:449` (and delete the dead commented-out duplicate at
   line 448, plus the now-unused `createDrillsForNewUser` method,
   `UserService.java:691-717`).
   Rows in `t_drill` will then continue to be created exactly the way they
   already are for drill *completion* — lazily, on demand, via
   `AthleteDrillItemProgressManager.completeDrill()` →
   `DrillService.create()` (`DrillService.java:124-144`), which already
   checks for an existing row before inserting. No new code path is needed;
   we are removing a redundant one.

2. **Add a unique constraint on `(user_id, drill_item_id)` in `t_drill`.**
   This guarantees one row per athlete-per-drill (no duplicates from races/
   retries) and makes the existence-check inside `DrillService.create()`
   a fast indexed lookup instead of a table scan. Add this as a new Flyway
   migration alongside the existing `V20260606120000__performance_indexes.sql`,
   e.g.:
   ```sql
   ALTER TABLE t_drill
       ADD CONSTRAINT uq_drill_user_drill_item UNIQUE (user_id, drill_item_id);
   ```
   (Run a duplicate-check/cleanup query first — see step 3 — otherwise this
   migration will fail if dupes already exist.)

3. **Clean up the placeholder rows that already exist — without touching real data.**
   We must be careful here: some `NOT-ATTEMPTED` rows in the table today might
   *also* be legitimate (e.g., an athlete who started a drill but the status
   wasn't updated yet, or any row that's been referenced elsewhere). The safe
   rule is: **only delete rows that are pure untouched placeholders** —
   i.e. `drill_status = 'NOT-ATTEMPTED'` AND `attempts_detected = 0` AND
   `attempts_reported = 0` AND `makes_detected = 0` AND `makes_reported = 0`
   AND no associated `t_drill_attempt_history` rows. Anything that doesn't
   meet *all* of these conditions is left untouched, because it represents
   real athlete activity.
   ```sql
   DELETE FROM t_drill d
   WHERE d.drill_status = 'NOT-ATTEMPTED'
     AND d.attempts_detected = 0
     AND d.attempts_reported = 0
     AND d.makes_detected = 0
     AND d.makes_reported = 0
     AND NOT EXISTS (
         SELECT 1 FROM t_drill_attempt_history h WHERE h.drill_id = d.id
     );
   ```
   This should be run as a one-off backfill migration (or an ops script run
   during a maintenance window), with a row-count check / backup beforehand
   so it's reversible if something looks off.

## Why this solves the problem and meets industry standard

- **No behavior change for athletes**: the drill list screen already renders
  "not attempted" drills from in-memory defaults, so removing the placeholder
  rows changes nothing the athlete sees.
- **No behavior change for drill completion**: `DrillService.create()` already
  creates a row on first real attempt, so progress tracking continues to work
  exactly as it does today.
- **Table growth becomes proportional to real activity**, not to
  `athletes × drills`, which directly removes the scaling/indexing problem we
  hit.
- **Login becomes lighter and safer under concurrency** — no more N inserts
  per login, no more SERIALIZABLE write storms at the moment of highest
  concurrent load.
- **The unique constraint enforces data integrity** going forward (one row per
  athlete-per-drill, guaranteed by the database, not just by application logic),
  which is the standard way to prevent the exact race-condition duplicates this
  design was vulnerable to.

## Suggested rollout order

1. Ship the code change (remove `createDrillsForNewUser` + its call site).
2. Run the cleanup `DELETE` as a guarded, backed-up maintenance step.
3. Add the unique constraint migration (will now succeed cleanly since dupes
   are gone).
4. Monitor `t_drill` row growth and login latency post-deploy to confirm the
   table now grows with real activity instead of with login volume.
