# 06/17/26

## Background (what happened)
We added a CI safety check that builds + tests the code on a clean machine before
deploying to EC2. It failed — not because of our code, but because it exposed two
things that were never actually in git and only existed on the EC2 box:
  - A dependency jar (tus-java-server) that was an unpublished SNAPSHOT.  -> FIXED
  - The entire `media/` source folder (43 .java files: video processing, the
    computer-vision/AI scoring code, S3 upload). It was hidden by a .gitignore rule.

The app works today only because EC2 has these files on its disk. Git does not.

## TODO

1) Push the workflow change to unblock deploys  (DO NOW)
   - Commit + push `.github/workflows/deploy.yml` (gate already removed).
   - Why: right now the safety check blocks ALL deploys, including the new
     drill-score feature. Removing it lets us ship again. Deploy still builds on
     EC2 and won't restart if the build breaks.

2) Get the `media/` source code into git  (IMPORTANT, do soon)
   - scp the two media/ folders off EC2 (main + test) and commit them:
       /var/www/html/pallbearer/pallbearer-core/src/main/java/.../media
       /var/www/html/pallbearer/pallbearer-core/src/test/java/.../media
   - Also check EC2 for missing test resource files:
       git ls-files --others --ignored --exclude-standard | grep -v '\.java$'
   - Why: this code lives in ONLY ONE place — that EC2 box. If the server dies,
     gets reimaged, or its disk fails, the media + AI-scoring code is gone for
     good (it's not in git history anywhere). It's only ~43 small text files, so
     there is no GitHub space problem. This is a backup/safety issue, not a
     feature issue.

3) Re-add the CI build+test gate  (after step 2)
   - Put the `build-and-test` job back in deploy.yml with `needs: build-and-test`
     (there's a TODO comment in the file marking the spot).
   - Why: once media/ is in git, a clean machine can build the project again, so
     the gate will pass. Then broken code can never reach EC2 — the build/tests
     must pass first.

## Done
- Fixed tus-java-server dependency (SNAPSHOT -> released 1.0.0-3.1).
- Fixed .gitignore so the media source folder CAN be committed.
- Drill-score "scoreAdjusted" feature finished (logic + tests + DB migration).

---

# 07/28/26 — Subscription end-date is not enforced at login (pre-existing bug)

Raised by the CRM team in §6a of the Sign-in-with-Google spec. This is NOT caused by
Google login — it affects password login (`/api/sso/crm-login`) in production today.
Parked here so the Google build isn't blocked on it. **Needs QA input before enforcing.**

## The bug

The payment gate at `SsoResource.java:397-407` tests only the `payment_status` STRING.
It never reads `subscription_end_date`, even though that column exists on
`t_crm_registration` (`CrmRegistrationRow:32`, `Optional<Long>`, epoch millis).

The CRM's own rule is stricter:
    Registration.is_active() == (status == 'ACTIVE' AND subscription_end_date > now)

And the CRM webhook sends `subscriptionEndDate` but NOT the registration's `status`,
so this backend has no way to learn that the CRM considers a registration EXPIRED.

Consequence: a registration whose end date has passed, but whose `payment_status` was
never rewritten, still passes the gate — **app access continues indefinitely.**

Stripe-backed subscriptions are fine (a canceled/expired event maps to EXPIRED/UNPAID).
The exposure is the non-Stripe grants, which nothing updates on a timer:
  - Coach signup        -> 365-day CLUB_PAID, payment_status='PAID', and
                           referenced_club_id is None so the club-expiry routine
                           never matches it. Most likely to be affected.
  - Japanese athlete    -> 60-day TRIAL
  - Web free-trial      -> 60-day SELF_PAY / TRIAL

CRM team confirmed there is no scheduled date-based expiry job on their side; the only
EXPIRED transitions are event-driven.

## Why we did NOT just fix it

Adding the check locks out, at the instant of deploy, every user whose date has lapsed
but who logs in fine today. That is a behaviour change on the login path that currently
works. We don't yet know if that's 5 users or 800.

## TODO

1) Measure the blast radius (DO FIRST — must run on EC2, no datasource config in repo)

   The spec's bare count:

       SELECT count(*) FROM t_crm_registration
       WHERE subscription_end_date < (extract(epoch from now()) * 1000)
         AND payment_status IN ('PAID','PARTIAL','TRIAL','TRAIL');

   Better — shows WHO, which changes the rollout plan:

       SELECT payment_status,
              count(*)                   AS affected,
              min(subscription_end_date) AS oldest_lapse,
              max(subscription_end_date) AS newest_lapse
       FROM t_crm_registration
       WHERE subscription_end_date < (extract(epoch from now()) * 1000)
         AND payment_status IN ('PAID','PARTIAL','TRIAL','TRAIL')
       GROUP BY payment_status;

   If it's mostly coach CLUB_PAID -> this is a coach-comms problem, go slow.
   If it's mostly lapsed TRIAL    -> those arguably should be locked out, go faster.

2) Let it run in shadow mode and read the logs (already built, see below)

   The check is IMPLEMENTED but INERT on /api/sso/google-login. Flag:

       auth.subscription.enddate.enforced   (default false)

   With the flag false it logs `SUBSCRIPTION_EXPIRED_SHADOW user=... status=... endDate=...`
   and lets the login through. Grep that line after a week: the real number is always
   smaller than the table count, because plenty of lapsed rows belong to people who
   stopped using the app and will never log in again.

3) Decide with QA, then flip the flag on EC2

   Flag flip is the cheap undo — prod runs Quarkus in dev mode, so a code rollback means
   a live-reload and 502s. Prefer the flag.

4) Only after that: apply the same check to /api/sso/crm-login

   DELIBERATELY NOT DONE YET. crm-login is what every shipped iOS build uses.
   Two blockers before touching it:
     - Blast radius unknown (step 1).
     - Spec §6b: confirm the SHIPPED iOS build degrades gracefully on an unknown
       `error_code`. If it crashes or shows a blank alert, return PAYMENT_REQUIRED on
       crm-login and keep SUBSCRIPTION_EXPIRED for the Google endpoint only.
   Google login is safe to enforce on immediately — no shipped build calls it.

5) Ask the CRM team for the source-side fix (they offered, awaiting our answer)

   - add `status` to the webhook payload
   - nightly CRM job flipping lapsed registrations to EXPIRED/UNPAID

   Take both. Their job fixes the source; our login gate is defence in depth for when
   the job fails. Gating at login is otherwise just compensating for stale data.

## Also from that spec, not ours but tracked

- TLS on 34.236.102.26 (§7). The app talks to this backend over plaintext HTTP with
  NSAllowsArbitraryLoads. Login credentials AND the Bearer token on every authenticated
  call afterwards travel unencrypted. CRM team's view, which I agree with: this is a
  bigger problem than anything Google sign-in introduces. Infra ticket.
- Sign in with Apple may be pulled into the same release by App Store guideline 4.8.
  Product decision, affects the release date, not the backend.

---

# 07/28/26 — Sign in with Google: state, and the two open decisions

## Backend state

BUILT, NOT YET VERIFIED. Six files, +584 lines:
  - NEW  CrmGoogleResult.java              (plain class, NOT a record — jandex 1.2.3)
  - NEW  GoogleLoginRequest.java           ({"credential": "<google id token>"})
  - MOD  CrmApiClient.validateGoogleCredential()
  - MOD  AuthErrorCode                     (+NO_ACCOUNT, NOT_LINKED, ACCOUNT_INACTIVE,
                                            SUBSCRIPTION_EXPIRED)
  - MOD  LoginRateLimiter.googleIpKey()    (separate bucket, see below)
  - MOD  SsoResource                       (POST /api/sso/google-login)

Deliberately a SEPARATE endpoint, not a branch inside crm-login: the request has no
username, the throttle ordering genuinely differs (IP-only until the CRM answers), and
the new error codes must never reach already-shipped iOS builds.

Google's failures use their own IP bucket. If they fed the shared one, a client
hammering Google sign-in would lock out EVERY login method for everyone behind that
NAT — a school or club on one public IP.

This backend needs NO Google config — no client ID, no Google library. It never
verifies the token; it passes it to the CRM. If a Google client ID ever appears in
this repo's config, something has gone wrong.

Verified locally: 5 of 6 files compile clean (javac exit 0).
NOT verified: SsoResource — it transitively imports the missing media package (see
06/17/26 above). Unrelated to Google.

MUST be built on EC2 before anyone says it works. Watch:
  1. the jandex step — the plain-class choice is only validated after compile succeeds
  2. that SsoResource compiles at all

Two bugs found and fixed in final review:
  - the CRM's `accountPendingDeletion` flag was parsed but ignored; we only checked the
    local t_user row. Deletion can be requested on the WEB CRM and is not guaranteed to
    be mirrored locally, so a user mid-grace could have been issued a token. Now trusts
    the CRM first, local row as fallback.
  - no null guard on the CRM-returned username; a malformed 200 would have been an NPE
    → 500. Prod live-reloads, so that is downtime, not a failed deploy.

## DECISION 1 — when to enforce the subscription end date

Owner: us + QA. See the 07/28/26 section above for the full write-up.
Status: check is BUILT and INERT on google-login. Flag
`auth.subscription.enddate.enforced` defaults false → logs SUBSCRIPTION_EXPIRED_SHADOW
and lets the login through. crm-login deliberately untouched.

Before flipping it:
  a. run the row count (query in the section above, must run on EC2)
  b. let shadow mode run ~1 week, grep SUBSCRIPTION_EXPIRED_SHADOW — the real number is
     always smaller than the table count
  c. notify whoever shows up in those logs
  d. flip the flag on EC2 (flag flip, not a redeploy — prod live-reloads and a rollback
     means 502s)

## DECISION 2 — do we want the CRM's source-side fix

Owner: us to answer, CRM to build. They offered, we have not replied.
  - add `status` to the webhook payload
  - nightly CRM job flipping lapsed registrations to EXPIRED/UNPAID

Recommendation: say yes to both. Their job fixes it at the source; our login gate is
defence in depth for when the job fails. Gating at login alone is just compensating for
stale data. Reply to them either way — they are waiting on us.

## ⚠ BEFORE iOS TESTING — the audience trap

The CRM changed its Google verifier to accept a LIST of client IDs instead of one.
Only the WEBSITE's ID is on that list today. The iOS ID gets added later.

A Google token is stamped with the ID of whichever app requested it. So:
  - a token grabbed from a browser (spec §8's test method) carries the WEB id → PASSES
  - a token from a real iPhone carries the iOS id → REJECTED as INVALID_GOOGLE_TOKEN

That failure reads as "bad token, try again" and points at nothing. Expect every
backend test to look green and the first real device to fail.

Checklist before blaming the app:
  [ ] iOS OAuth client created in the SAME Google Cloud project as the web client
  [ ] that iOS client ID added to the CRM's accepted-audience list on EC2 .env
  [ ] CRM deployed FIRST, then the .env change, then restart — their stated order.
      Adding the ID before the code is deployed breaks WEB login.
  [ ] decode one real device token at jwt.io and confirm the `aud` claim matches what
      the CRM accepts. One minute, saves a day of finger-pointing.

Do NOT read green §8 curl tests as "the app will work". That is only true once the iOS
ID is on the list and a real device token has been tested end to end.
