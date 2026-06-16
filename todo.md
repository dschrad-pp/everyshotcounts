# 06/15/26 — Auth hardening follow-ups (SECURITY — do before public launch)

Context: shipped login UX/security improvements to /api/sso (crm-login, coach-login,
admin-login): machine-readable `error_code` field, 401+INVALID_CREDENTIALS for bad
creds (no user enumeration), 403 REGISTRATION_INCOMPLETE / PAYMENT_REQUIRED, 503 for
CRM/upstream outage, 429 + Retry-After brute-force lockout (LoginRateLimiter), and no
more stack-trace leakage. The items below were found during that work but deferred —
we deliberately LEFT them as-is for now so this push doesn't break prod.

## 1. Hardcoded CRM service-account creds (LEFT IN ON PURPOSE — fix before launch)
File: pallbearer-api/.../crm/CrmApiClient.java, getAccessToken()
- Falls back to `rojan` / `rojan1234` if CRM_USERNAME / CRM_PASSWORD env vars are unset.
- Confirmed on EC2 (2026-06-15): those env vars are NOT set and there is no
  crm.username/crm.password in application.properties → prod has been authenticating
  to the CRM as `rojan` this whole time.
- The secret is in git history regardless, so the `rojan` CRM account must be ROTATED.
- Fix when ready: set CRM_USERNAME / CRM_PASSWORD in the systemd drop-in
  (/etc/systemd/system/quarkus-backend.service.d/override.conf), same pattern as the
  APNS_PRODUCTION note below, then remove the hardcoded fallback so it fails closed.
  The app already reads env vars via System.getenv here; once set they take effect
  (restart required — dev mode reads them at process start).

## 2. crm.ios.api.key inline default (CHECK — possible leaked key)
File: pallbearer-api/src/main/resources/application.properties:125
- Form is `crm.ios.api.key=${CRM_IOS_API_KEY:<default>}`. If <default> is a real key it
  is committed to git. Confirm; if real, drop the inline default (fail closed) + rotate.

## 3. Prod is running `mvn quarkus:dev` (DEV MODE in production)
- quarkus-backend.service runs `quarkus:dev` from /var/www/html/pallbearer/pallbearer-api
  with a JDWP debug agent listening on localhost:5005 and live hot-reload.
- Risks: debug port, hot-reload, verbose error pages, slower, reads config live from the
  source tree. Move to a packaged build (`quarkus:prod` / `java -jar ...-runner.jar`,
  prod profile) for launch. Config then comes from the built app + env, not src/.

## 4. Set CRM_USERNAME / CRM_PASSWORD on EC2 (ties #1 together)
- Until set, #1's fallback is the only thing keeping CRM auth alive. Setting them is the
  prerequisite for removing the hardcoded creds.

## 5. (Optional) Rate limiter is process-local
- LoginRateLimiter is in-memory (fine for the current single EC2 box, fails safe on
  restart). If the backend is ever horizontally scaled, back it with a shared store
  (e.g. Redis) so per-account/IP limits hold across instances.

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
