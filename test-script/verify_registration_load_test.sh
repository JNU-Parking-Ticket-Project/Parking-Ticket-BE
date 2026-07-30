#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPOSITORY_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
MYSQL_SERVICE="${MYSQL_SERVICE:-mysql}"
REDIS_SERVICE="${REDIS_SERVICE:-redis}"
LOAD_TEST_DB_USER="${LOAD_TEST_DB_USER:-econo}"
LOAD_TEST_DB_PASSWORD="${LOAD_TEST_DB_PASSWORD:-econo}"
LOAD_TEST_DB_NAME="${LOAD_TEST_DB_NAME:-ticket}"
SUMMARY_PATH="${SUMMARY_PATH:-test-script/registration-load-summary.json}"
RUNTIME_PATH="${RUNTIME_PATH:-test-script/registration-load-runtime.json}"
WAIT_TIMEOUT_SECONDS="${WAIT_TIMEOUT_SECONDS:-120}"
EXPECTED_SECTOR_COUNT="${EXPECTED_SECTOR_COUNT:-5}"
EXPECTED_ISSUE_AMOUNT_PER_SECTOR="${EXPECTED_ISSUE_AMOUNT_PER_SECTOR:-60}"
summary_event_id=""
summary_run_id=""

cd "${REPOSITORY_ROOT}"

if ! command -v jq >/dev/null 2>&1; then
  echo "jq is required to verify the k6 summary and JSON application logs" >&2
  exit 2
fi

if [[ -f "${SUMMARY_PATH}" ]]; then
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

if [[ ! "${EXPECTED_SECTOR_COUNT}" =~ ^[1-9][0-9]*$
  || ! "${EXPECTED_ISSUE_AMOUNT_PER_SECTOR}" =~ ^[1-9][0-9]*$ ]]; then
  echo "EXPECTED_SECTOR_COUNT and EXPECTED_ISSUE_AMOUNT_PER_SECTOR must be positive integers" >&2
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

failures=0

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

assert_equal() {
  local label="$1"
  local expected="$2"
  local actual="$3"
  if [[ "${actual}" != "${expected}" ]]; then
    echo "FAIL: ${label} (expected=${expected}, actual=${actual})" >&2
    failures=$((failures + 1))
  else
    echo "PASS: ${label} (${actual})"
  fi
}

assert_at_least_one() {
  local label="$1"
  local actual="$2"
  if [[ ! "${actual}" =~ ^[0-9]+$ ]] || (( actual < 1 )); then
    echo "FAIL: ${label} (${actual})" >&2
    failures=$((failures + 1))
  else
    echo "PASS: ${label} (${actual})"
  fi
}

xinfo_field() {
  local info="$1"
  local field="$2"
  awk -v target="${field}" 'previous == target { print; exit } { previous = $0 }' <<< "${info}"
}

json_log_count_after_line() {
  local path="$1"
  local start_line="$2"
  local thread_prefix="$3"
  local message_prefix="$4"
  local first_line=$((start_line + 1))

  if [[ ! -f "${path}" ]]; then
    echo -1
    return
  fi

  tail -n "+${first_line}" "${path}" \
    | jq -s \
      --arg threadPrefix "${thread_prefix}" \
      --arg messagePrefix "${message_prefix}" \
      '[.[]
        | select((.message // "") | startswith($messagePrefix))
        | select($threadPrefix == "" or ((.thread // "") | startswith($threadPrefix)))
      ] | length'
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

expected_accepted="${EXPECTED_ACCEPTED:-$((EXPECTED_SECTOR_COUNT * EXPECTED_ISSUE_AMOUNT_PER_SECTOR))}"
if [[ -z "${EXPECTED_ACCEPTED:-}"
  && "${summary_event_id}" == "${EVENT_ID}"
  && "${summary_run_id}" == "${RUN_ID}" ]]; then
  summary_expected_accepted="$(jq -r '.metadata.expectedAccepted // empty' "${SUMMARY_PATH}")"
  if [[ -n "${summary_expected_accepted}" ]]; then
    expected_accepted="${summary_expected_accepted}"
  fi
fi

email_pattern="load-${RUN_ID}-%@example.com"
actual_accepted="$(mysql_query "
  SELECT COUNT(*)
  FROM registration_tb
  WHERE event_id = ${EVENT_ID}
    AND is_saved = 1
    AND email LIKE '${email_pattern}';")"

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

  summary_planned="$(jq -r '.metadata.plannedRequests' "${SUMMARY_PATH}")"
  summary_expected_no_stock="$(jq -r '.metadata.expectedNoStock' "${SUMMARY_PATH}")"
  summary_attempted="$(jq -r '.result.attempted' "${SUMMARY_PATH}")"
  summary_accepted="$(jq -r '.result.accepted' "${SUMMARY_PATH}")"
  summary_no_stock="$(jq -r '.result.noStock' "${SUMMARY_PATH}")"
  summary_unexpected="$(jq -r '.result.unexpected' "${SUMMARY_PATH}")"
  summary_server_errors="$(jq -r '.result.serverErrors' "${SUMMARY_PATH}")"
  assert_equal "all planned HTTP requests were attempted" "${summary_planned}" "${summary_attempted}"
  assert_equal "HTTP accepted count matches configured capacity" "${expected_accepted}" "${summary_accepted}"
  assert_equal "excess HTTP requests were rejected as no-stock" "${summary_expected_no_stock}" "${summary_no_stock}"
  assert_zero "unexpected HTTP responses" "${summary_unexpected}"
  assert_zero "HTTP 5xx responses" "${summary_server_errors}"
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

sector_shape="$(mysql_query "
  SELECT COUNT(*), COALESCE(MIN(issue_amount), 0), COALESCE(MAX(issue_amount), 0),
         COALESCE(SUM(issue_amount), 0)
  FROM sector
  WHERE event_id = ${EVENT_ID} AND is_deleted = 0;")"
IFS=$'\t' read -r actual_sector_count min_issue_amount max_issue_amount total_issue_amount <<< "${sector_shape}"
assert_equal "event has the configured sector count" "${EXPECTED_SECTOR_COUNT}" "${actual_sector_count}"
assert_equal "minimum issue amount per sector" "${EXPECTED_ISSUE_AMOUNT_PER_SECTOR}" "${min_issue_amount}"
assert_equal "maximum issue amount per sector" "${EXPECTED_ISSUE_AMOUNT_PER_SECTOR}" "${max_issue_amount}"
assert_equal "total issue amount matches expected accepted count" "${expected_accepted}" "${total_issue_amount}"

invalid_sector_registration_counts="$(mysql_query "
  SELECT COUNT(*)
  FROM (
    SELECT sector.sector_id
    FROM sector
    LEFT JOIN registration_tb registration
      ON registration.sector_id = sector.sector_id
      AND registration.event_id = ${EVENT_ID}
      AND registration.email LIKE '${email_pattern}'
      AND registration.is_saved = 1
    WHERE sector.event_id = ${EVENT_ID} AND sector.is_deleted = 0
    GROUP BY sector.sector_id, sector.issue_amount
    HAVING COUNT(registration.id) <> sector.issue_amount
  ) invalid_sector_count;")"
assert_zero "every sector persists exactly its issue amount" "${invalid_sector_registration_counts}"

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
      AND email LIKE '${email_pattern}'
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
      AND email LIKE '${email_pattern}'
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

legacy_job_count="$(mysql_query "
  SELECT COUNT(*)
  FROM QRTZ_JOB_DETAILS
  WHERE LEFT(JOB_NAME, LENGTH('PROCESS_QUEUE_DATA_JOB')) = 'PROCESS_QUEUE_DATA_JOB';")"
legacy_trigger_count="$(mysql_query "
  SELECT COUNT(*)
  FROM QRTZ_TRIGGERS
  WHERE LEFT(JOB_NAME, LENGTH('PROCESS_QUEUE_DATA_JOB')) = 'PROCESS_QUEUE_DATA_JOB';")"
assert_zero "legacy PROCESS_QUEUE_DATA_JOB is absent from JDBC JobStore" "${legacy_job_count}"
assert_zero "legacy PROCESS_QUEUE_DATA_JOB trigger is absent from JDBC JobStore" "${legacy_trigger_count}"

legacy_job_source="Ticket-Batch/src/main/java/com/jnu/ticketbatch/config/ProcessQueueDataJob.java"
if [[ -e "${legacy_job_source}" ]]; then
  echo "FAIL: legacy Quartz polling source still exists (${legacy_job_source})" >&2
  failures=$((failures + 1))
else
  echo "PASS: legacy Quartz polling source is absent"
fi

stream_group_info="$(redis_command XINFO GROUPS "${STREAM_KEY}" || true)"
stream_group_name="$(xinfo_field "${stream_group_info}" name)"
stream_group_consumers="$(xinfo_field "${stream_group_info}" consumers)"
stream_group_pending="$(xinfo_field "${stream_group_info}" pending)"
stream_group_entries_read="$(xinfo_field "${stream_group_info}" entries-read)"
stream_group_lag="$(xinfo_field "${stream_group_info}" lag)"
assert_equal "Stream Consumer Group exists" "${STREAM_GROUP}" "${stream_group_name}"
assert_at_least_one "Stream group has a blocking consumer" "${stream_group_consumers}"
assert_zero "Stream group pending is drained" "${stream_group_pending:-missing}"
assert_equal "Stream group delivered every accepted message" "${expected_accepted}" "${stream_group_entries_read}"
assert_zero "Stream group lag is drained" "${stream_group_lag:-missing}"

stream_consumer_info="$(redis_command XINFO CONSUMERS "${STREAM_KEY}" "${STREAM_GROUP}" || true)"
blocking_consumer_count="$(
  awk -v prefix="쿠폰 발급 소비자-" -v eventId="${EVENT_ID}" '
    previous == "name" && index($0, prefix) == 1 && $0 ~ ("-" eventId "$") { count++ }
    { previous = $0 }
    END { print count + 0 }
  ' <<< "${stream_consumer_info}"
)"
assert_at_least_one "event-specific Stream consumer name is registered" "${blocking_consumer_count}"

if [[ ! -f "${RUNTIME_PATH}" ]]; then
  echo "FAIL: runtime evidence boundary is missing (${RUNTIME_PATH})" >&2
  failures=$((failures + 1))
else
  runtime_run_id="$(jq -r '.runId // empty' "${RUNTIME_PATH}")"
  runtime_event_id="$(jq -r '.eventId // empty' "${RUNTIME_PATH}")"
  if [[ "${runtime_run_id}" != "${RUN_ID}" || "${runtime_event_id}" != "${EVENT_ID}" ]]; then
    echo "FAIL: runtime evidence boundary belongs to another run" >&2
    failures=$((failures + 1))
  else
    process_log_path="$(jq -r '.processLogPath' "${RUNTIME_PATH}")"
    process_log_start_line="$(jq -r '.processLogStartLine' "${RUNTIME_PATH}")"
    deadline=$((SECONDS + WAIT_TIMEOUT_SECONDS))
    while true; do
      worker_saved_count="$(
        json_log_count_after_line \
          "${process_log_path}" \
          "${process_log_start_line}" \
          "redis-stream-worker-" \
          "Registration saved."
      )"
      if [[ "${worker_saved_count}" == "${actual_accepted}" || SECONDS -ge deadline ]]; then
        break
      fi
      sleep 1
    done
    total_saved_log_count="$(
      json_log_count_after_line \
        "${process_log_path}" \
        "${process_log_start_line}" \
        "" \
        "Registration saved."
    )"
    assert_equal "DB saves ran on redis-stream-worker threads" "${actual_accepted}" "${worker_saved_count}"
    assert_equal "no registration save ran through another execution path" "${worker_saved_count}" "${total_saved_log_count}"
  fi
fi

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

db_remaining_total="$(mysql_query "
  SELECT COALESCE(SUM(remaining_amount), 0)
  FROM sector
  WHERE event_id = ${EVENT_ID} AND is_deleted = 0;")"
assert_zero "DB remaining stock is exhausted" "${db_remaining_total}"

if [[ "${failures}" != "0" ]]; then
  echo "Load-test verification failed with ${failures} problem(s)." >&2
  exit 1
fi

echo "All asynchronous registration invariants passed (${actual_accepted} persisted registrations)."
