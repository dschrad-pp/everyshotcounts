#!/usr/bin/env bash

set -euo pipefail

MEDIA_ROOT="${MEDIA_ROOT:-/home/ankit/Downloads/thrones-development/media}"
S3_BUCKET="${PALLBEARER_STORAGE_S3_BUCKET:-}"
S3_REGION="${PALLBEARER_STORAGE_S3_REGION:-${AWS_REGION:-us-east-1}}"
S3_PUBLIC_BASE_URL="${PALLBEARER_STORAGE_S3_PUBLIC_BASE_URL:-}"
S3_ENDPOINT="${PALLBEARER_STORAGE_S3_ENDPOINT:-}"
DRY_RUN="${DRY_RUN:-false}"
DELETE_LOCAL_AFTER_UPLOAD="${DELETE_LOCAL_AFTER_UPLOAD:-false}"
AWS_CLI_EXTRA_ARGS="${AWS_CLI_EXTRA_ARGS:-}"
FAILED_ROWS_LOG="${FAILED_ROWS_LOG:-./migrate-media-to-s3.failures.log}"

if [[ -z "${S3_BUCKET}" ]]; then
  echo "Missing PALLBEARER_STORAGE_S3_BUCKET"
  exit 1
fi

if ! command -v aws >/dev/null 2>&1; then
  echo "aws CLI is required"
  exit 1
fi

if ! command -v psql >/dev/null 2>&1; then
  echo "psql is required"
  exit 1
fi

trim_trailing_slash() {
  local value="$1"
  value="${value%/}"
  echo "$value"
}

sql_escape() {
  printf "%s" "$1" | sed "s/'/''/g"
}

build_public_url() {
  local object_key="$1"
  if [[ -n "${S3_PUBLIC_BASE_URL}" ]]; then
    local base_url
    base_url="$(trim_trailing_slash "${S3_PUBLIC_BASE_URL}")"
    echo "${base_url}/${object_key}"
  else
    echo "https://${S3_BUCKET}.s3.${S3_REGION}.amazonaws.com/${object_key}"
  fi
}

relative_key_from_path() {
  local local_path="$1"
  local normalized_root
  normalized_root="$(trim_trailing_slash "${MEDIA_ROOT}")"

  if [[ "${local_path}" == "${normalized_root}/"* ]]; then
    echo "${local_path#${normalized_root}/}"
    return 0
  fi

  echo ""
  return 1
}

aws_cp() {
  local local_path="$1"
  local object_key="$2"
  local destination="s3://${S3_BUCKET}/${object_key}"
  local -a args=("s3" "cp" "--region" "${S3_REGION}" "${local_path}" "${destination}")

  if [[ -n "${S3_ENDPOINT}" ]]; then
    args+=("--endpoint-url" "${S3_ENDPOINT}")
  fi

  if [[ -n "${AWS_CLI_EXTRA_ARGS}" ]]; then
    # Intentional word splitting for caller-provided AWS CLI flags.
    # shellcheck disable=SC2206
    local extra_args=( ${AWS_CLI_EXTRA_ARGS} )
    args+=("${extra_args[@]}")
  fi

  if [[ "${DRY_RUN}" == "true" ]]; then
    echo "[dry-run] aws ${args[*]}"
    return 0
  fi

  aws "${args[@]}"
}

run_sql() {
  local sql="$1"
  if [[ -n "${DATABASE_URL:-}" ]]; then
    psql "${DATABASE_URL}" -v ON_ERROR_STOP=1 -c "${sql}" >/dev/null
  else
    psql -v ON_ERROR_STOP=1 -c "${sql}" >/dev/null
  fi
}

delete_local_file() {
  local local_path="$1"

  if [[ "${DELETE_LOCAL_AFTER_UPLOAD}" != "true" ]]; then
    return 0
  fi

  if [[ "${DRY_RUN}" == "true" ]]; then
    echo "[dry-run] rm -f ${local_path}"
    return 0
  fi

  if ! rm -f "${local_path}"; then
    echo "WARN delete failed for ${local_path}" >&2
  fi
}

log_failure() {
  local row_type="$1"
  local row_id="$2"
  local local_path="$3"
  local reason="$4"
  printf "%s\t%s\t%s\t%s\n" "${row_type}" "${row_id}" "${local_path}" "${reason}" >> "${FAILED_ROWS_LOG}"
}

query_rows() {
  local sql="$1"
  if [[ -n "${DATABASE_URL:-}" ]]; then
    psql "${DATABASE_URL}" -v ON_ERROR_STOP=1 -At -F $'\t' -c "${sql}"
  else
    psql -v ON_ERROR_STOP=1 -At -F $'\t' -c "${sql}"
  fi
}

process_media_rows() {
  local processed=0
  local skipped=0
  local failed=0

  while IFS=$'\t' read -r media_id local_path; do
    [[ -z "${media_id}" ]] && continue

    if [[ ! -f "${local_path}" ]]; then
      echo "Skipping media ${media_id}: file not found at ${local_path}"
      skipped=$((skipped + 1))
      continue
    fi

    local object_key
    object_key="$(relative_key_from_path "${local_path}")" || true
    if [[ -z "${object_key}" ]]; then
      echo "Skipping media ${media_id}: path is outside MEDIA_ROOT (${local_path})"
      skipped=$((skipped + 1))
      continue
    fi

    if migrate_media_row "${media_id}" "${local_path}" "${object_key}"; then
      processed=$((processed + 1))
    else
      echo "Failed media ${media_id}: ${local_path}" >&2
      log_failure "media" "${media_id}" "${local_path}" "migration_failed"
      failed=$((failed + 1))
    fi
  done < <(query_rows "SELECT id, content_url
                         FROM t_media
                        WHERE content_url IS NOT NULL
                          AND LENGTH(TRIM(content_url)) > 0
                          AND content_url LIKE '/%';")

  echo "Media rows migrated: ${processed}, skipped: ${skipped}, failed: ${failed}"
  return "${failed}"
}

process_thumbnail_rows() {
  local processed=0
  local skipped=0
  local failed=0

  while IFS=$'\t' read -r drill_item_id local_path; do
    [[ -z "${drill_item_id}" ]] && continue

    if [[ ! -f "${local_path}" ]]; then
      echo "Skipping drill item ${drill_item_id}: file not found at ${local_path}"
      skipped=$((skipped + 1))
      continue
    fi

    local object_key
    object_key="$(relative_key_from_path "${local_path}")" || true
    if [[ -z "${object_key}" ]]; then
      echo "Skipping drill item ${drill_item_id}: path is outside MEDIA_ROOT (${local_path})"
      skipped=$((skipped + 1))
      continue
    fi

    if migrate_thumbnail_row "${drill_item_id}" "${local_path}" "${object_key}"; then
      processed=$((processed + 1))
    else
      echo "Failed thumbnail ${drill_item_id}: ${local_path}" >&2
      log_failure "thumbnail" "${drill_item_id}" "${local_path}" "migration_failed"
      failed=$((failed + 1))
    fi
  done < <(query_rows "SELECT id, media_thumbnail
                         FROM t_drill_item
                        WHERE media_thumbnail IS NOT NULL
                          AND LENGTH(TRIM(media_thumbnail)) > 0
                          AND media_thumbnail LIKE '/%';")

  echo "Thumbnail rows migrated: ${processed}, skipped: ${skipped}, failed: ${failed}"
  return "${failed}"
}

migrate_media_row() {
  local media_id="$1"
  local local_path="$2"
  local object_key="$3"

  local public_url
  public_url="$(build_public_url "${object_key}")"

  aws_cp "${local_path}" "${object_key}"

  local escaped_url
  escaped_url="$(sql_escape "${public_url}")"
  local sql="UPDATE t_media
                SET content_url = '${escaped_url}',
                    modification_date = EXTRACT(EPOCH FROM NOW()) * 1000,
                    version = version + 1
              WHERE id = '${media_id}';"

  if [[ "${DRY_RUN}" == "true" ]]; then
    echo "[dry-run] ${sql}"
  else
    run_sql "${sql}"
  fi

  delete_local_file "${local_path}"
  echo "Migrated media ${media_id} -> ${public_url}"
}

migrate_thumbnail_row() {
  local drill_item_id="$1"
  local local_path="$2"
  local object_key="$3"

  local public_url
  public_url="$(build_public_url "${object_key}")"

  aws_cp "${local_path}" "${object_key}"

  local escaped_url
  escaped_url="$(sql_escape "${public_url}")"
  local sql="UPDATE t_drill_item
                SET media_thumbnail = '${escaped_url}',
                    modification_date = EXTRACT(EPOCH FROM NOW()) * 1000,
                    version = version + 1
              WHERE id = '${drill_item_id}';"

  if [[ "${DRY_RUN}" == "true" ]]; then
    echo "[dry-run] ${sql}"
  else
    run_sql "${sql}"
  fi

  delete_local_file "${local_path}"
  echo "Migrated thumbnail ${drill_item_id} -> ${public_url}"
}

echo "Starting media migration to S3"
echo "MEDIA_ROOT=${MEDIA_ROOT}"
echo "S3_BUCKET=${S3_BUCKET}"
echo "S3_REGION=${S3_REGION}"
echo "DRY_RUN=${DRY_RUN}"
echo "DELETE_LOCAL_AFTER_UPLOAD=${DELETE_LOCAL_AFTER_UPLOAD}"
echo "FAILED_ROWS_LOG=${FAILED_ROWS_LOG}"

: > "${FAILED_ROWS_LOG}"

media_failed=0
thumbnail_failed=0

process_media_rows || media_failed=$?
process_thumbnail_rows || thumbnail_failed=$?

total_failed=$((media_failed + thumbnail_failed))
echo "Migration complete. total_failed=${total_failed}"

if [[ "${total_failed}" -gt 0 ]]; then
  echo "Some rows failed. See ${FAILED_ROWS_LOG}" >&2
  exit 1
fi
