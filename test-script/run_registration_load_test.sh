#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPOSITORY_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
MYSQL_SERVICE="${MYSQL_SERVICE:-mysql}"
LOAD_TEST_DB_USER="${LOAD_TEST_DB_USER:-econo}"
LOAD_TEST_DB_PASSWORD="${LOAD_TEST_DB_PASSWORD:-econo}"
LOAD_TEST_DB_NAME="${LOAD_TEST_DB_NAME:-ticket}"
K6_BIN="${K6_BIN:-k6}"
K6_API_ADDRESS="${K6_API_ADDRESS:-127.0.0.1:0}"
RUNTIME_PATH="${RUNTIME_PATH:-test-script/registration-load-runtime.json}"
PROCESS_LOG_PATH="${PROCESS_LOG_PATH:-Ticket-Api/logs/$(date +%F)-process.log}"

line_count() {
  local path="$1"
  if [[ -f "${path}" ]]; then
    wc -l < "${path}" | tr -d ' '
  else
    echo 0
  fi
}

generate_run_id() {
  local run_date
  local last_sequence
  local next_sequence

  run_date="$(date +%Y%m%d)"
  if ! last_sequence="$(
    docker compose exec -T "${MYSQL_SERVICE}" \
      mysql --default-character-set=utf8mb4 \
      "-u${LOAD_TEST_DB_USER}" "-p${LOAD_TEST_DB_PASSWORD}" \
      --batch --skip-column-names "${LOAD_TEST_DB_NAME}" \
      -e "
        SELECT COALESCE(
          MAX(CAST(SUBSTRING_INDEX(SUBSTRING_INDEX(email, '-', 3), '-', -1) AS UNSIGNED)),
          0
        )
        FROM user_tb
        WHERE email REGEXP '^load-${run_date}-[0-9]+-[0-9]{6}@example[.]com$';" \
      2>/dev/null
  )"; then
    echo "Failed to read the next load-test sequence from MySQL." >&2
    echo "Check Docker Compose and LOAD_TEST_DB_* settings, or provide RUN_ID explicitly." >&2
    exit 1
  fi

  if [[ ! "${last_sequence}" =~ ^[0-9]+$ ]]; then
    echo "MySQL returned an invalid load-test sequence: ${last_sequence}" >&2
    exit 1
  fi

  printf -v next_sequence '%02d' "$((10#${last_sequence} + 1))"
  printf '%s-%s' "${run_date}" "${next_sequence}"
}

cd "${REPOSITORY_ROOT}"

if [[ -z "${RUN_ID:-}" ]]; then
  RUN_ID="$(generate_run_id)"
fi

if [[ ! "${RUN_ID}" =~ ^[A-Za-z0-9-]+$ ]]; then
  echo "RUN_ID may contain only letters, numbers, and hyphens" >&2
  exit 2
fi

if [[ -z "${EVENT_ID:-}" || ! "${EVENT_ID}" =~ ^[1-9][0-9]*$ ]]; then
  echo "EVENT_ID must be provided as a positive integer" >&2
  exit 2
fi

if ! command -v jq >/dev/null 2>&1; then
  echo "jq is required to record the runtime evidence boundary" >&2
  exit 2
fi

runtime_temp_path="${RUNTIME_PATH}.tmp"
jq -n \
  --arg runId "${RUN_ID}" \
  --argjson eventId "${EVENT_ID}" \
  --arg processLogPath "${PROCESS_LOG_PATH}" \
  --argjson processLogStartLine "$(line_count "${PROCESS_LOG_PATH}")" \
  --arg startedAt "$(date -Iseconds)" \
  '{
    runId: $runId,
    eventId: $eventId,
    processLogPath: $processLogPath,
    processLogStartLine: $processLogStartLine,
    startedAt: $startedAt
  }' > "${runtime_temp_path}"
mv "${runtime_temp_path}" "${RUNTIME_PATH}"

export RUN_ID
echo "Load-test RUN_ID=${RUN_ID}"
echo "Runtime evidence boundary=${RUNTIME_PATH}"

exec "${K6_BIN}" run --address "${K6_API_ADDRESS}" "$@" "${SCRIPT_DIR}/k6_test_registration.js"
