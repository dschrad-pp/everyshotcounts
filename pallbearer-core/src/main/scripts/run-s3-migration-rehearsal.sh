#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="/home/ankit/Downloads/thrones-development"
REHEARSAL_SCRIPT="${ROOT_DIR}/pallbearer/pallbearer-core/src/main/scripts/migrate-media-to-s3-rehearsal.sh"
SEED_SQL="${ROOT_DIR}/migration-rehearsal-test-data.sql"
CLEANUP_SQL="${ROOT_DIR}/migration-rehearsal-cleanup.sql"
MEDIA_ROOT="${MEDIA_ROOT:-${ROOT_DIR}/media-migration-sample}"
DATABASE_URL="${DATABASE_URL:-postgresql://postgres:db#2700@localhost:5432/thrones_db}"
DRY_RUN="${DRY_RUN:-true}"
DELETE_LOCAL_AFTER_UPLOAD="${DELETE_LOCAL_AFTER_UPLOAD:-false}"
AUTO_CLEANUP_TEST_ROWS="${AUTO_CLEANUP_TEST_ROWS:-true}"

if ! command -v psql >/dev/null 2>&1; then
  echo "psql is required"
  exit 1
fi

if ! command -v aws >/dev/null 2>&1; then
  echo "aws CLI is required"
  exit 1
fi

if [[ -z "${PALLBEARER_STORAGE_S3_BUCKET:-}" ]]; then
  echo "Missing PALLBEARER_STORAGE_S3_BUCKET in environment"
  exit 1
fi

run_psql_file() {
  local file_path="$1"
  if [[ -n "${DATABASE_URL:-}" ]]; then
    psql "${DATABASE_URL}" -v ON_ERROR_STOP=1 -f "${file_path}"
  else
    PGPASSWORD='db#2700' psql -h localhost -U postgres -d thrones_db -v ON_ERROR_STOP=1 -f "${file_path}"
  fi
}

cleanup() {
  if [[ "${AUTO_CLEANUP_TEST_ROWS}" == "true" ]]; then
    echo "Cleaning up rehearsal DB rows"
    run_psql_file "${CLEANUP_SQL}" >/dev/null
  fi
}

trap cleanup EXIT

echo "Seeding rehearsal DB rows"
run_psql_file "${SEED_SQL}" >/dev/null

echo "Running rehearsal migration"
MEDIA_ROOT="${MEDIA_ROOT}" \
DATABASE_URL="${DATABASE_URL}" \
DRY_RUN="${DRY_RUN}" \
DELETE_LOCAL_AFTER_UPLOAD="${DELETE_LOCAL_AFTER_UPLOAD}" \
bash "${REHEARSAL_SCRIPT}"

echo "Rehearsal flow complete"
