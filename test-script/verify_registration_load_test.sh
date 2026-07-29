#!/usr/bin/env bash

set -euo pipefail

MYSQL_SERVICE="${MYSQL_SERVICE:-mysql}"
REDIS_SERVICE="${REDIS_SERVICE:-redis}"
LOAD_TEST_DB_USER="${LOAD_TEST_DB_USER:-econo}"
LOAD_TEST_DB_PASSWORD="${LOAD_TEST_DB_PASSWORD:-econo}"
LOAD_TEST_DB_NAME="${LOAD_TEST_DB_NAME:-ticket}"
SUMMARY_PATH="${SUMMARY_PATH:-test-script/registration-load-summary.json}"
WAIT_TIMEOUT_SECONDS="${WAIT_TIMEOUT_SECONDS:-120}"
summary_event_id=""
summary_run_id=""

if [[ -f "${SUMMARY_PATH}" ]] && command -v jq >/dev/null 2>&1; then
  summary_event_id="$(jq -r '.metadata.eventId // empty' "${SUMMARY_PATH}")"
  summary_run_id="$(jq -r '.metadata.runId // empty' "${SUMMARY_PATH}")"
fi

EVENT_ID="${EVENT_ID:-${summary_event_id}}"
RUN_ID="${RUN_ID:-${summary_run_id}}"

if [[ -z "${EVENT_ID}" || -z "${RUN_ID}" ]]; then
  echo "EVENT_ID and RUN_ID are required." >&2
  echo "Run the load test first, keep ${SUMMARY_PATH}, or provide both values explicitly." >&2
  exit 2
fi

STREAM_KEY="쿠폰 발급 스트림:{${EVENT_ID}}"
STREAM_GROUP="쿠폰 발급 그룹"

if [[ ! "${EVENT_ID}" =~ ^[1-9][0-9]*$ ]]; then
  echo "EVENT_ID must be a positive integer" >&2
  exit 2
fi

if [[ ! "${RUN_ID}" =~ ^[A-Za-z0-9-]+$ ]]; then
  echo "RUN_ID may contain only letters, numbers, and hyphens" >&2
  exit 2
fi

mysql_query() {
  docker compose exec -T "${MYSQL_SERVICE}" \
    mysql --default-character-set=utf8mb4 \
    "-u${LOAD_TEST_DB_USER}" "-p${LOAD_TEST_DB_PASSWORD}" \
    --batch --skip-column-names "${LOAD_TEST_DB_NAME}" \
    -e "$1" 2>/dev/null
}

redis_command() {
  docker compose exec -T "${REDIS_SERVICE}" redis-cli --raw "$@" 2>/dev/null
}

if [[ "$(redis_command PING || true)" != "PONG" ]]; then
  echo "FAIL: Redis is unavailable; asynchronous state cannot be verified" >&2
  exit 1
fi

assert_zero() {
  local label="$1"
  local value="$2"
  if [[ "${value}" != "0" ]]; then
    echo "FAIL: ${label} (${value})" >&2
    failures=$((failures + 1))
  else
    echo "PASS: ${label}"
  fi
}

stream_pending_count() {
  local result
  result="$(redis_command XPENDING "${STREAM_KEY}" "${STREAM_GROUP}" | head -n 1 || true)"
  if [[ "${result}" =~ ^[0-9]+$ ]]; then
    echo "${result}"
  else
    echo "0"
  fi
}

echo "Waiting for Redis Stream consumer to commit and ACK all messages..."
deadline=$((SECONDS + WAIT_TIMEOUT_SECONDS))
while true; do
  stream_length="$(redis_command XLEN "${STREAM_KEY}" || echo 0)"
  pending_count="$(stream_pending_count)"
  [[ "${stream_length}" =~ ^[0-9]+$ ]] || stream_length=0

  if [[ "${stream_length}" == "0" && "${pending_count}" == "0" ]]; then
    break
  fi
  if (( SECONDS >= deadline )); then
    echo "FAIL: Redis Stream did not drain within ${WAIT_TIMEOUT_SECONDS}s (length=${stream_length}, pending=${pending_count})" >&2
    exit 1
  fi
  sleep 1
done

expected_accepted="${EXPECTED_ACCEPTED:-}"
if [[ -z "${expected_accepted}"
  && "${summary_event_id}" == "${EVENT_ID}"
  && "${summary_run_id}" == "${RUN_ID}" ]]; then
  expected_accepted="$(jq -r '.result.accepted // empty' "${SUMMARY_PATH}")"
fi

email_pattern="load-${RUN_ID}-%@example.com"
actual_accepted="$(mysql_query "
  SELECT COUNT(*)
  FROM registration_tb
  WHERE event_id = ${EVENT_ID}
    AND is_saved = 1
    AND email LIKE '${email_pattern}';")"

failures=0
echo "Checking registrations for runId=${RUN_ID}, eventId=${EVENT_ID}..."

if [[ "${summary_event_id}" == "${EVENT_ID}" && "${summary_run_id}" == "${RUN_ID}" ]]; then
  failed_thresholds="$(jq -r '
    .k6.metrics
    | to_entries[]
    | select(.value.thresholds != null)
    | select([.value.thresholds[]?.ok] | any(. == false))
    | .key
  ' "${SUMMARY_PATH}")"
  if [[ -z "${failed_thresholds}" ]]; then
    echo "PASS: all k6 load thresholds passed"
  else
    while IFS= read -r failed_threshold; do
      [[ -n "${failed_threshold}" ]] || continue
      echo "FAIL: k6 threshold crossed (${failed_threshold})" >&2
      failures=$((failures + 1))
    done <<< "${failed_thresholds}"
  fi
else
  echo "WARN: matching k6 summary unavailable; load thresholds were not verified" >&2
fi

if [[ -n "${expected_accepted}" ]]; then
  if [[ "${actual_accepted}" == "${expected_accepted}" ]]; then
    echo "PASS: persisted registrations match accepted HTTP responses (${actual_accepted})"
  else
    echo "FAIL: accepted HTTP responses=${expected_accepted}, persisted registrations=${actual_accepted}" >&2
    failures=$((failures + 1))
  fi
else
  echo "WARN: expected accepted count unavailable; set EXPECTED_ACCEPTED or keep ${SUMMARY_PATH}" >&2
fi

null_decisions="$(mysql_query "
  SELECT COUNT(*)
  FROM registration_tb
  WHERE event_id = ${EVENT_ID}
    AND email LIKE '${email_pattern}'
    AND (position IS NULL OR result_status IS NULL OR sequence IS NULL);")"
assert_zero "all persisted registrations have position, resultStatus, and sequence" "${null_decisions}"

duplicate_positions="$(mysql_query "
  SELECT COUNT(*)
  FROM (
    SELECT sector_id, position
    FROM registration_tb
    WHERE event_id = ${EVENT_ID}
    GROUP BY sector_id, position
    HAVING COUNT(*) > 1
  ) duplicated;")"
assert_zero "positions are unique within each sector" "${duplicate_positions}"

non_contiguous_sectors="$(mysql_query "
  SELECT COUNT(*)
  FROM (
    SELECT sector_id
    FROM registration_tb
    WHERE event_id = ${EVENT_ID}
    GROUP BY sector_id
    HAVING MIN(position) <> 1
       OR MAX(position) <> COUNT(*)
       OR COUNT(DISTINCT position) <> COUNT(*)
  ) invalid_sector;")"
assert_zero "positions are contiguous from 1 within each sector" "${non_contiguous_sectors}"

invalid_decisions="$(mysql_query "
  SELECT COUNT(*)
  FROM registration_tb registration
  JOIN sector ON sector.sector_id = registration.sector_id
  WHERE registration.event_id = ${EVENT_ID}
    AND registration.email LIKE '${email_pattern}'
    AND (
      (registration.position <= sector.init_sector_capacity
        AND (registration.result_status <> '합격' OR registration.sequence <> -2))
      OR
      (registration.position > sector.init_sector_capacity
        AND registration.position <= sector.issue_amount
        AND (registration.result_status <> '예비'
          OR registration.sequence <> registration.position - sector.init_sector_capacity))
      OR registration.position > sector.issue_amount
    );")"
assert_zero "position maps to the intended success/reserve result" "${invalid_decisions}"

invalid_outbox_counts="$(mysql_query "
  SELECT COUNT(*)
  FROM (
    SELECT registration.id
    FROM registration_tb registration
    LEFT JOIN email_outbox outbox ON outbox.registration_id = registration.id
    WHERE registration.event_id = ${EVENT_ID}
      AND registration.email LIKE '${email_pattern}'
    GROUP BY registration.id
    HAVING COUNT(outbox.id) <> 1
  ) invalid_outbox_count;")"
assert_zero "every persisted registration has exactly one outbox row" "${invalid_outbox_counts}"

mismatched_outboxes="$(mysql_query "
  SELECT COUNT(*)
  FROM registration_tb registration
  JOIN email_outbox outbox ON outbox.registration_id = registration.id
  WHERE registration.event_id = ${EVENT_ID}
    AND registration.email LIKE '${email_pattern}'
    AND (outbox.event_id <> registration.event_id
      OR outbox.result_status <> registration.result_status
      OR outbox.sequence <> registration.sequence);")"
assert_zero "outbox result data matches registration result data" "${mismatched_outboxes}"

user_result_mismatches="$(mysql_query "
  SELECT COUNT(*)
  FROM registration_tb registration
  JOIN user_tb user ON user.user_id = registration.user_id
  WHERE registration.event_id = ${EVENT_ID}
    AND registration.email LIKE '${email_pattern}'
    AND (user.status <> registration.result_status OR user.sequence <> registration.sequence);")"
assert_zero "user result matches registration result" "${user_result_mismatches}"

echo "Waiting for the scheduled Redis-to-DB stock sync..."
deadline=$((SECONDS + WAIT_TIMEOUT_SECONDS))
while true; do
  stock_mismatches=0
  while IFS=$'\t' read -r sector_id db_remaining; do
    [[ -n "${sector_id}" ]] || continue
    redis_remaining="$(redis_command GET "parking-ticket:event:{${EVENT_ID}}:sector:${sector_id}:stock" || true)"
    if [[ -z "${redis_remaining}" || "${redis_remaining}" != "${db_remaining}" ]]; then
      stock_mismatches=$((stock_mismatches + 1))
    fi
  done < <(mysql_query "
    SELECT sector_id, remaining_amount
    FROM sector
    WHERE event_id = ${EVENT_ID} AND is_deleted = 0;")

  if [[ "${stock_mismatches}" == "0" ]]; then
    echo "PASS: Redis and DB remaining stock match"
    break
  fi
  if (( SECONDS >= deadline )); then
    echo "FAIL: Redis stock is missing or differs from DB in ${stock_mismatches} sector(s)" >&2
    failures=$((failures + 1))
    break
  fi
  sleep 1
done

if [[ "${failures}" != "0" ]]; then
  echo "Load-test verification failed with ${failures} problem(s)." >&2
  exit 1
fi

echo "All asynchronous registration invariants passed (${actual_accepted} persisted registrations)."
