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
