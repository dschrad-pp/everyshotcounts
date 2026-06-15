# 06/15/26 — Coaches should only see PASSING attempts

Goal: in the coach-facing attempt history, hide attempts the athlete didn't pass
(show only passing ones). NOTE: do this as a *display filter* on coach endpoints
only — the failed attempts (and their videos) should still be stored, because the
per-attempt media fix now links each attempt's own video. Don't delete data.

## 0. First decide the pass criterion (BLOCKER — codebase is inconsistent)
There is NO pass/fail or status column on t_drill_attempt_history. "Pass" must be
derived per attempt by comparing makes to the drill ITEM's passing_score.
- Existing definition #1 (per drill): CoachNotificationService.java:90 uses
  `makesDetected < passingScore` => fail  (i.e. pass = makesDetected >= passingScore)
- Existing definition #2 (level test, aggregate): AthleteDrillItemLockManager.java:337
  uses `(totalMakes*100/totalPassingScore) >= 70%` across the whole test set.
- Tags/stats memory note says the canonical stat source is makes_REPORTED, not
  makes_detected.
ACTION: pick ONE definition (recommend `makesReported >= passingScore` for a single
attempt, OR reuse CoachNotificationService's makesDetected for consistency) and apply
it everywhere. Confirm with product which makes field is authoritative.

## 1. Filter the coach attempt-history assembly (server-side)
File: pallbearer-core/.../jdbi/service/AthleteDrillService.java
- findLatestAttemptedDrillsForAthleteUnderCoach (~line 522-531, the attemptHistory
  list we just changed to return all attempts): after building the list, keep only
  attempts where makes >= drillItem passing_score. passingScore is already in scope
  via drillItemRow.get().getPassingScore() (used ~line 563).
- Check the second attempt-history assembly in the same file (~line 372-387) — if it
  is coach-facing, apply the same filter; if athlete-facing, leave it.
- Guard nulls: if passingScore is null or 0, decide behavior (recommend: show the
  attempt, since pass can't be determined) so drills without a configured passing
  score don't silently vanish.

## 2. Do NOT filter athlete-facing endpoints
- DrillService.findByIdWithHistory (used by PUT .../complete and the media PATCH
  responses) must keep returning ALL attempts — the athlete should see their own
  failed attempts. Only coach endpoints filter.

## 3. Level-test drills need a decision
Per-attempt `makes >= passingScore` does not match the 70%-aggregate rule used for
level tests (AthleteDrillItemLockManager.java:337). Decide whether level-test
attempts are filtered per-attempt the same way, or excluded from this filtering.

## 4. Edge case: all attempts failed
If every attempt is filtered out, the coach gets an empty attemptHistory list.
Confirm the iOS coach screen renders "no passing attempts yet" gracefully rather
than blank/error.

## 5. (Optional, longer term) efficiency
Currently this is a Java-side filter after fetching all rows. If attempt volumes
grow, add a coach-specific SQL query that joins t_drill_item.passing_score and
filters makes >= passing_score in the DB. Or add a computed/stored `passed` flag on
t_drill_attempt_history at insert time (migration) so pass status doesn't have to be
re-derived on every read.

# 06/14/26

- We need to update apns.production=false to true when we deploy to app store/testflight. Steps: 

Option A — systemd env var (recommended)
This is the deploy-safe way. Quarkus lets any env var override a property, and apns.production maps to APNS_PRODUCTION. The override file is never touched by git pull or mvn, so it survives every deploy — unlike editing application.properties.


sudo systemctl edit quarkus-backend.service
Add under [Service]:


Environment=APNS_PRODUCTION=true
Then apply (config is only read at startup, so a restart is required):


sudo systemctl daemon-reload
sudo systemctl restart quarkus-backend.service
sudo journalctl -u quarkus-backend.service -f | grep -i apns
Confirm you see: APNs service initialized (production=true, bundleId=com.every.shot.counts). To go back, change it to false (or delete the line) and restart.

