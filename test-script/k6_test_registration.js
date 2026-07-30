import http from "k6/http";
import exec from "k6/execution";
import { check, fail, sleep } from "k6";
import { Counter, Gauge, Rate, Trend } from "k6/metrics";

const BASE_URL = (__ENV.BASE_URL || "http://localhost:8080").replace(/\/+$/, "");
const EVENT_ID = requiredPositiveInteger("EVENT_ID");
const RUN_ID = requiredRunId();
const CAPTCHA_ANSWER = requiredEnvironmentValue("CAPTCHA_ANSWER");
// This config schedules request starts; it does not claim completed HTTP throughput.
const TARGET_RPS = positiveInteger("TARGET_RPS", 1000);
const LOAD_DURATION_SECONDS = positiveInteger("LOAD_DURATION_SECONDS", 1);
const TOTAL_REQUESTS = TARGET_RPS * LOAD_DURATION_SECONDS;
const EXPECTED_SECTOR_COUNT = positiveInteger("EXPECTED_SECTOR_COUNT", 5);
const EXPECTED_ISSUE_AMOUNT_PER_SECTOR = positiveInteger(
  "EXPECTED_ISSUE_AMOUNT_PER_SECTOR",
  60,
);
const EXPECTED_ACCEPTED = positiveInteger(
  "EXPECTED_ACCEPTED",
  EXPECTED_SECTOR_COUNT * EXPECTED_ISSUE_AMOUNT_PER_SECTOR,
);
if (EXPECTED_ACCEPTED > TOTAL_REQUESTS) {
  throw new Error("EXPECTED_ACCEPTED cannot exceed the planned request count");
}
const EXPECTED_NO_STOCK = TOTAL_REQUESTS - EXPECTED_ACCEPTED;
const BURST_WINDOW_MS = nonNegativeInteger(
  "BURST_WINDOW_MS",
  LOAD_DURATION_SECONDS * 1000,
);
const REQUESTS_PER_VU = positiveInteger("REQUESTS_PER_VU", 5);
const LOAD_VUS = Math.ceil(TOTAL_REQUESTS / REQUESTS_PER_VU);
const PREPARE_VUS_PER_SECOND = positiveInteger("PREPARE_VUS_PER_SECOND", 10);
const PREPARATION_BUFFER_SECONDS = positiveInteger("PREPARATION_BUFFER_SECONDS", 30);
const PREPARATION_WINDOW_SECONDS =
  Math.ceil(LOAD_VUS / PREPARE_VUS_PER_SECOND) + PREPARATION_BUFFER_SECONDS;
const MIN_EVENT_REMAINING_SECONDS = positiveInteger(
  "MIN_EVENT_REMAINING_SECONDS",
  PREPARATION_WINDOW_SECONDS + LOAD_DURATION_SECONDS + 30,
);
const MAX_START_DELAY_P95_MS = positiveInteger("MAX_START_DELAY_P95_MS", 500);
const PASSWORD = __ENV.USER_PASSWORD || "Password!123";
const INCLUDE_DEPARTMENT = (__ENV.INCLUDE_DEPARTMENT || "true") !== "false";
const REUSE_HTTP_CONNECTIONS = (__ENV.REUSE_HTTP_CONNECTIONS || "false") === "true";
const MONITOR_HIKARI = (__ENV.MONITOR_HIKARI || "false") === "true";
const HIKARI_MONITOR_BEFORE_MS = nonNegativeInteger("HIKARI_MONITOR_BEFORE_MS", 1000);
const HIKARI_MONITOR_AFTER_MS = positiveInteger("HIKARI_MONITOR_AFTER_MS", 10000);
const HIKARI_MONITOR_INTERVAL_MS = positiveInteger("HIKARI_MONITOR_INTERVAL_MS", 50);
const SUMMARY_PATH =
  __ENV.SUMMARY_PATH || "test-script/registration-load-summary.json";
const CONFIGURED_SECTOR_IDS = parseSectorIds(__ENV.SECTOR_IDS);

const attempted = new Counter("registration_attempted");
const accepted = new Counter("registration_accepted");
const noStock = new Counter("registration_no_stock");
const unexpected = new Counter("registration_unexpected");
const transportErrors = new Counter("registration_transport_errors");
const serverErrors = new Counter("registration_server_errors");
const unexpectedClientErrors = new Counter("registration_unexpected_client_errors");
const unexpectedRate = new Rate("registration_unexpected_rate");
const registrationDuration = new Trend("registration_request_duration", true);
const registrationAcceptedDuration = new Trend("registration_accepted_duration", true);
const registrationNoStockDuration = new Trend("registration_no_stock_duration", true);
const registrationStartDelay = new Trend("registration_start_delay", true);
const hikariActive = new Gauge("hikari_active_connections");
const hikariPending = new Gauge("hikari_pending_threads");
const hikariTimeout = new Gauge("hikari_connection_timeouts");
let loggedUnexpectedResponse = false;

const scenarios = {
  registration_burst: {
    executor: "per-vu-iterations",
    vus: LOAD_VUS,
    iterations: 1,
    maxDuration:
      __ENV.MAX_DURATION ||
      `${PREPARATION_WINDOW_SECONDS + LOAD_DURATION_SECONDS + 120}s`,
  },
};

if (MONITOR_HIKARI) {
  scenarios.hikari_monitor = {
    executor: "shared-iterations",
    vus: 1,
    iterations: 1,
    exec: "monitorHikari",
    maxDuration: `${PREPARATION_WINDOW_SECONDS + 60}s`,
  };
}

export const options = {
  setupTimeout: __ENV.SETUP_TIMEOUT || "5m",
  // Prepared users can wait longer than Tomcat's keep-alive timeout before the burst.
  noConnectionReuse: !REUSE_HTTP_CONNECTIONS,
  batch: Math.max(REQUESTS_PER_VU, 20),
  batchPerHost: Math.max(REQUESTS_PER_VU, 20),
  scenarios,
  thresholds: {
    [`http_req_failed{phase:prepare}`]: ["rate==0"],
    registration_attempted: [`count==${TOTAL_REQUESTS}`],
    registration_accepted: [`count==${EXPECTED_ACCEPTED}`],
    registration_no_stock: [`count==${EXPECTED_NO_STOCK}`],
    registration_unexpected_rate: ["rate==0"],
    registration_request_duration: ["p(95)<5000"],
    registration_start_delay: [`p(95)<${MAX_START_DELAY_P95_MS}`],
  },
  summaryTrendStats: ["min", "avg", "med", "p(90)", "p(95)", "p(99)", "max"],
};

export function setup() {
  const active = readActiveEvent();
  const sectors = selectSectors(readActiveSectors());
  validateEventWindow(active.dateTimePeriod);

  const firstUser = loginSingleUser(0);
  firstUser.captchaCode = requestCaptcha(firstUser.accessToken, 0);
  const burstAt = Date.now() + PREPARATION_WINDOW_SECONDS * 1000;

  console.log(
    [
      `Preparing ${TOTAL_REQUESTS} users for run ${RUN_ID}.`,
      `eventId=${EVENT_ID}`,
      `sectorIds=${sectors.map((sector) => sector.id).join(",")}`,
      `expectedSectorCount=${EXPECTED_SECTOR_COUNT}`,
      `expectedIssueAmountPerSector=${EXPECTED_ISSUE_AMOUNT_PER_SECTOR}`,
      `configuredIssueAmount=${sectors.reduce((sum, sector) => sum + sector.issueAmount, 0)}`,
      `loadVUs=${LOAD_VUS}`,
      `requestsPerVU=${REQUESTS_PER_VU}`,
      `reuseHttpConnections=${REUSE_HTTP_CONNECTIONS}`,
      `burstWindowMs=${BURST_WINDOW_MS}`,
      `burstStartsIn=${PREPARATION_WINDOW_SECONDS}s`,
    ].join(" "),
  );

  return {
    eventId: EVENT_ID,
    firstUser,
    sectorIds: sectors.map((sector) => sector.id),
    burstAt,
  };
}

export default function (data) {
  const batchIndex = Number(exec.scenario.iterationInTest);
  const startIndex = batchIndex * REQUESTS_PER_VU;
  const endIndex = Math.min(startIndex + REQUESTS_PER_VU, TOTAL_REQUESTS);
  if (startIndex >= TOTAL_REQUESTS) {
    return;
  }

  sleep(batchIndex / PREPARE_VUS_PER_SECOND);
  const users = prepareUsersForBatch(data, startIndex, endIndex);

  const scheduledAt =
    data.burstAt +
    Math.floor((startIndex / TOTAL_REQUESTS) * BURST_WINDOW_MS);
  sleepUntil(scheduledAt);
  registrationStartDelay.add(Math.max(0, Date.now() - scheduledAt));

  const requests = users.map((user) => {
    const sectorId = data.sectorIds[user.index % data.sectorIds.length];
    return {
      method: "POST",
      url: `${BASE_URL}/api/v1/registration/${data.eventId}`,
      body: JSON.stringify(registrationPayload(user.index, sectorId, user.captchaCode)),
      params: {
        headers: authorizedJsonHeaders(user.accessToken),
        tags: {
          phase: "load",
          endpoint: "final-registration",
          sector_id: String(sectorId),
        },
        responseCallback: http.expectedStatuses(200, 400),
      },
    };
  });

  const responses = http.batch(requests);
  responses.forEach((response, offset) => {
    const user = users[offset];
    const sectorId = data.sectorIds[user.index % data.sectorIds.length];
    recordRegistrationResult(response, sectorId);
  });
}

export function monitorHikari(data) {
  sleepUntil(data.burstAt - HIKARI_MONITOR_BEFORE_MS);
  const monitorUntil = data.burstAt + HIKARI_MONITOR_AFTER_MS;

  while (Date.now() <= monitorUntil) {
    const names = [
      "hikaricp.connections.active",
      "hikaricp.connections.pending",
      "hikaricp.connections.timeout",
    ];
    const responses = http.batch(
      names.map((name) => ({
        method: "GET",
        url: `${BASE_URL}/api/actuator/metrics/${name}`,
        params: {
          tags: { phase: "monitor", endpoint: name },
          responseCallback: http.expectedStatuses(200),
        },
      })),
    );

    hikariActive.add(actuatorMeasurement(responses[0]));
    hikariPending.add(actuatorMeasurement(responses[1]));
    hikariTimeout.add(actuatorMeasurement(responses[2]));
    sleep(HIKARI_MONITOR_INTERVAL_MS / 1000);
  }
}

export function teardown(data) {
  console.log(
    `HTTP load is complete. Verify async persistence with: RUN_ID=${RUN_ID} EVENT_ID=${data.eventId} test-script/verify_registration_load_test.sh`,
  );
}

export function handleSummary(data) {
  const attemptedCount = metricValue(data, "registration_attempted", "count");
  const acceptedCount = metricValue(data, "registration_accepted", "count");
  const noStockCount = metricValue(data, "registration_no_stock", "count");
  const unexpectedCount = metricValue(data, "registration_unexpected", "count");
  const transportErrorCount = metricValue(
    data,
    "registration_transport_errors",
    "count",
  );
  const serverErrorCount = metricValue(data, "registration_server_errors", "count");
  const unexpectedClientErrorCount = metricValue(
    data,
    "registration_unexpected_client_errors",
    "count",
  );
  const durationP95 = metricValue(data, "registration_request_duration", "p(95)");
  const durationAvg = metricValue(data, "registration_request_duration", "avg");
  const durationMedian = metricValue(data, "registration_request_duration", "med");
  const durationP99 = metricValue(data, "registration_request_duration", "p(99)");
  const acceptedDurationAvg = metricValue(data, "registration_accepted_duration", "avg");
  const acceptedDurationP95 = metricValue(
    data,
    "registration_accepted_duration",
    "p(95)",
  );
  const noStockDurationAvg = metricValue(
    data,
    "registration_no_stock_duration",
    "avg",
  );
  const noStockDurationP95 = metricValue(
    data,
    "registration_no_stock_duration",
    "p(95)",
  );
  const startDelayP95 = metricValue(data, "registration_start_delay", "p(95)");
  const hikariActiveMax = monitoredMetricValue(
    data,
    "hikari_active_connections",
    "max",
  );
  const hikariPendingMax = monitoredMetricValue(
    data,
    "hikari_pending_threads",
    "max",
  );
  const hikariTimeoutMax = monitoredMetricValue(
    data,
    "hikari_connection_timeouts",
    "max",
  );

  const report = {
    metadata: {
      runId: RUN_ID,
      eventId: EVENT_ID,
      baseUrl: BASE_URL,
      targetStartRatePerSecond: TARGET_RPS,
      loadDurationSeconds: LOAD_DURATION_SECONDS,
      burstWindowMs: BURST_WINDOW_MS,
      plannedRequests: TOTAL_REQUESTS,
      expectedSectorCount: EXPECTED_SECTOR_COUNT,
      expectedIssueAmountPerSector: EXPECTED_ISSUE_AMOUNT_PER_SECTOR,
      expectedAccepted: EXPECTED_ACCEPTED,
      expectedNoStock: EXPECTED_NO_STOCK,
      requestsPerVu: REQUESTS_PER_VU,
      loadVUs: LOAD_VUS,
      reuseHttpConnections: REUSE_HTTP_CONNECTIONS,
      monitorHikari: MONITOR_HIKARI,
      configuredSectorIds: CONFIGURED_SECTOR_IDS,
    },
    result: {
      attempted: attemptedCount,
      accepted: acceptedCount,
      noStock: noStockCount,
      unexpected: unexpectedCount,
      transportErrors: transportErrorCount,
      serverErrors: serverErrorCount,
      unexpectedClientErrors: unexpectedClientErrorCount,
      registrationDurationAvgMs: durationAvg,
      registrationDurationMedianMs: durationMedian,
      registrationDurationP95Ms: durationP95,
      registrationDurationP99Ms: durationP99,
      acceptedDurationAvgMs: acceptedDurationAvg,
      acceptedDurationP95Ms: acceptedDurationP95,
      noStockDurationAvgMs: noStockDurationAvg,
      noStockDurationP95Ms: noStockDurationP95,
      registrationStartDelayP95Ms: startDelayP95,
      hikariActiveMax,
      hikariPendingMax,
      hikariTimeoutMax,
    },
    k6: data,
  };

  const stdout = [
    "",
    "Registration load-test result",
    `  run id:               ${RUN_ID}`,
    `  planned requests:     ${TOTAL_REQUESTS}`,
    `  expected accepted:    ${EXPECTED_ACCEPTED}`,
    `  expected no stock:    ${EXPECTED_NO_STOCK}`,
    `  attempted requests:   ${attemptedCount}`,
    `  accepted (200):       ${acceptedCount}`,
    `  no stock (400):       ${noStockCount}`,
    `  unexpected:           ${unexpectedCount}`,
    `    transport errors:   ${transportErrorCount}`,
    `    server errors:      ${serverErrorCount}`,
    `    other responses:    ${unexpectedClientErrorCount}`,
    `  registration avg ms:  ${durationAvg}`,
    `  registration p50 ms:  ${durationMedian}`,
    `  registration p95 ms:  ${durationP95}`,
    `  registration p99 ms:  ${durationP99}`,
    `  accepted avg ms:      ${acceptedDurationAvg}`,
    `  accepted p95 ms:      ${acceptedDurationP95}`,
    `  no stock avg ms:      ${noStockDurationAvg}`,
    `  no stock p95 ms:      ${noStockDurationP95}`,
    `  start delay p95 ms:   ${startDelayP95}`,
    `  Hikari active max:    ${displayMonitoredMetric(hikariActiveMax)}`,
    `  Hikari pending max:   ${displayMonitoredMetric(hikariPendingMax)}`,
    `  Hikari timeout max:   ${displayMonitoredMetric(hikariTimeoutMax)}`,
    `  summary file:         ${SUMMARY_PATH}`,
    "",
  ].join("\n");

  return {
    stdout,
    [SUMMARY_PATH]: JSON.stringify(report, null, 2),
  };
}

function prepareUsersForBatch(data, startIndex, endIndex) {
  const users = [];
  let nextIndex = startIndex;

  if (startIndex === 0) {
    users.push(data.firstUser);
    nextIndex = 1;
  }

  if (nextIndex < endIndex) {
    const newUsers = loginUserBatch(nextIndex, endIndex);
    requestCaptchaBatch(newUsers);
    users.push(...newUsers);
  }

  return users;
}

function recordRegistrationResult(response, sectorId) {
  attempted.add(1, { sector_id: String(sectorId) });
  registrationDuration.add(response.timings.duration, { sector_id: String(sectorId) });

  const status = Number(response.status || 0);
  const code = response.status === 400 ? responseErrorCode(response) : null;
  const isAccepted = status === 200;
  const isNoStock = status === 400 && code === "EVENT_400_2";
  const isUnexpected = !isAccepted && !isNoStock;
  const metricTags = {
    sector_id: String(sectorId),
    status: String(status),
    code: code || response.error_code || "UNKNOWN",
  };

  if (isAccepted) {
    accepted.add(1, { sector_id: String(sectorId) });
    registrationAcceptedDuration.add(response.timings.duration, {
      sector_id: String(sectorId),
    });
  } else if (isNoStock) {
    noStock.add(1, { sector_id: String(sectorId) });
    registrationNoStockDuration.add(response.timings.duration, {
      sector_id: String(sectorId),
    });
  } else {
    unexpected.add(1, metricTags);
    recordUnexpectedResponse(response, metricTags);
  }

  unexpectedRate.add(isUnexpected);
  check(response, {
    "registration was accepted or rejected because stock was exhausted": () =>
      !isUnexpected,
  });
}

function recordUnexpectedResponse(response, metricTags) {
  const status = Number(response.status || 0);
  if (status === 0) {
    transportErrors.add(1, metricTags);
  } else if (status >= 500) {
    serverErrors.add(1, metricTags);
  } else {
    unexpectedClientErrors.add(1, metricTags);
  }

  if (!loggedUnexpectedResponse) {
    loggedUnexpectedResponse = true;
    console.warn(
      [
        "Unexpected registration response.",
        `status=${status}`,
        `errorCode=${response.error_code || "none"}`,
        `error=${response.error || "none"}`,
        `body=${String(response.body || "").slice(0, 300)}`,
      ].join(" "),
    );
  }
}

function readActiveEvent() {
  const response = http.get(`${BASE_URL}/api/v1/events/period`, {
    tags: { phase: "prepare", endpoint: "active-event" },
  });
  assertResponseStatus(response, 200, "active event lookup");

  const event = responseJson(response, "active event lookup");
  if (Number(event.eventId) !== EVENT_ID) {
    fail(`Configured EVENT_ID=${EVENT_ID}, but active published event is ${event.eventId}`);
  }
  return event;
}

function readActiveSectors() {
  const response = http.get(`${BASE_URL}/api/v1/sectors`, {
    tags: { phase: "prepare", endpoint: "active-sectors" },
  });
  assertResponseStatus(response, 200, "active sector lookup");

  const sectors = responseJson(response, "active sector lookup");
  if (!Array.isArray(sectors) || sectors.length === 0) {
    fail("The active published event has no sectors");
  }
  return sectors;
}

function selectSectors(sectors) {
  const selected = CONFIGURED_SECTOR_IDS
    ? sectors.filter((sector) => CONFIGURED_SECTOR_IDS.includes(Number(sector.id)))
    : sectors;

  if (CONFIGURED_SECTOR_IDS && selected.length !== CONFIGURED_SECTOR_IDS.length) {
    const available = sectors.map((sector) => sector.id).join(",");
    fail(`SECTOR_IDS contains an inactive sector. Available sector ids: ${available}`);
  }
  if (selected.length !== EXPECTED_SECTOR_COUNT) {
    fail(
      `Expected ${EXPECTED_SECTOR_COUNT} sectors, but the active event exposes ${selected.length}`,
    );
  }
  if (
    selected.some(
      (sector) => Number(sector.issueAmount) !== EXPECTED_ISSUE_AMOUNT_PER_SECTOR,
    )
  ) {
    fail(
      `Every selected sector must have issueAmount=${EXPECTED_ISSUE_AMOUNT_PER_SECTOR}`,
    );
  }

  return selected.map((sector) => ({
    id: Number(sector.id),
    issueAmount: Number(sector.issueAmount),
  }));
}

function validateEventWindow(period) {
  if (!period || !period.startAt || !period.endAt) {
    fail("The active event response does not contain a valid dateTimePeriod");
  }

  const now = Date.now();
  const startAt = parseSeoulDateTime(period.startAt);
  const endAt = parseSeoulDateTime(period.endAt);

  if (now < startAt) {
    fail("The event is published but not OPEN yet");
  }
  if (now >= endAt) {
    fail("The event has already ended");
  }

  const remainingSeconds = Math.floor((endAt - now) / 1000);
  if (remainingSeconds < MIN_EVENT_REMAINING_SECONDS) {
    fail(
      `Only ${remainingSeconds}s remain in the event. At least ${MIN_EVENT_REMAINING_SECONDS}s is required for preparation and load execution`,
    );
  }
}

function loginSingleUser(index) {
  const email = loadTestEmail(index);
  const response = http.post(
    `${BASE_URL}/api/v1/auth/login`,
    loginPayload(email),
    jsonParams("prepare", "login"),
  );
  assertResponseStatus(response, 200, `login for ${email}`);
  return preparedUser(index, email, response, `login for ${email}`);
}

function loginUserBatch(startIndex, endIndex) {
  const requests = [];
  const emails = [];

  for (let index = startIndex; index < endIndex; index += 1) {
    const email = loadTestEmail(index);
    emails.push(email);
    requests.push({
      method: "POST",
      url: `${BASE_URL}/api/v1/auth/login`,
      body: loginPayload(email),
      params: jsonParams("prepare", "login"),
    });
  }

  const responses = http.batch(requests);
  return responses.map((response, offset) => {
    const index = startIndex + offset;
    const email = emails[offset];
    assertResponseStatus(response, 200, `login for ${email}`);
    return preparedUser(index, email, response, `login for ${email}`);
  });
}

function requestCaptcha(accessToken, index) {
  const response = http.get(`${BASE_URL}/api/v1/captcha`, {
    headers: authorizedJsonHeaders(accessToken),
    tags: { phase: "prepare", endpoint: "captcha" },
  });
  assertResponseStatus(response, 200, `captcha request for user ${index}`);
  return captchaCode(response, `captcha request for user ${index}`);
}

function requestCaptchaBatch(users) {
  const requests = users.map((user) => ({
    method: "GET",
    url: `${BASE_URL}/api/v1/captcha`,
    params: {
      headers: authorizedJsonHeaders(user.accessToken),
      tags: { phase: "prepare", endpoint: "captcha" },
    },
  }));

  const responses = http.batch(requests);
  responses.forEach((response, offset) => {
    const user = users[offset];
    assertResponseStatus(response, 200, `captcha request for user ${user.index}`);
    user.captchaCode = captchaCode(response, `captcha request for user ${user.index}`);
  });
}

function preparedUser(index, email, response, label) {
  const body = responseJson(response, label);
  if (!body.accessToken) {
    fail(`${label} did not return accessToken`);
  }
  return { index, email, accessToken: body.accessToken, captchaCode: null };
}

function captchaCode(response, label) {
  const body = responseJson(response, label);
  if (!body.captchaCode) {
    fail(`${label} did not return captchaCode`);
  }
  return body.captchaCode;
}

function registrationPayload(index, sectorId, code) {
  const serial = String(index % 900000).padStart(6, "0");
  const phoneSerial = String(index % 100000000).padStart(8, "0");
  const payload = {
    name: `load-user-${index}`,
    studentNum: serial,
    affiliation: "공과대학",
    carNum: `12가${String(index % 10000).padStart(4, "0")}`,
    isLight: index % 2 === 0,
    phoneNum: `010-${phoneSerial.slice(0, 4)}-${phoneSerial.slice(4)}`,
    selectSectorId: sectorId,
    captchaCode: code,
    captchaAnswer: CAPTCHA_ANSWER,
  };
  if (INCLUDE_DEPARTMENT) {
    payload.department = "컴퓨터정보통신공학과";
  }
  return payload;
}

function loginPayload(email) {
  return JSON.stringify({ email, pwd: PASSWORD });
}

function loadTestEmail(index) {
  return `load-${RUN_ID}-${String(index).padStart(6, "0")}@example.com`;
}

function authorizedJsonHeaders(accessToken) {
  return {
    "Content-Type": "application/json",
    Authorization: `Bearer ${accessToken}`,
  };
}

function jsonParams(phase, endpoint) {
  return {
    headers: { "Content-Type": "application/json" },
    tags: { phase, endpoint },
  };
}

function sleepUntil(epochMilliseconds) {
  const delayMilliseconds = epochMilliseconds - Date.now();
  if (delayMilliseconds > 0) {
    sleep(delayMilliseconds / 1000);
  }
}

function responseJson(response, label) {
  try {
    return response.json();
  } catch (error) {
    fail(`${label} returned invalid JSON: ${String(response.body).slice(0, 300)}`);
  }
}

function responseErrorCode(response) {
  try {
    return response.json().code || null;
  } catch (error) {
    return null;
  }
}

function assertResponseStatus(response, expectedStatus, label) {
  if (response.status !== expectedStatus) {
    fail(
      `${label} failed with HTTP ${response.status}: ${String(response.body).slice(0, 500)}`,
    );
  }
}

function parseSeoulDateTime(value) {
  const millisecondPrecision = String(value).replace(/(\.\d{3})\d+$/, "$1");
  const parsed = Date.parse(`${millisecondPrecision}+09:00`);
  if (Number.isNaN(parsed)) {
    fail(`Cannot parse event date-time: ${value}`);
  }
  return parsed;
}

function parseSectorIds(raw) {
  if (!raw) {
    return null;
  }
  const ids = raw.split(",").map((value) => Number(value.trim()));
  if (ids.some((id) => !Number.isInteger(id) || id <= 0)) {
    throw new Error("SECTOR_IDS must be a comma-separated list of positive integers");
  }
  return [...new Set(ids)];
}

function requiredEnvironmentValue(name) {
  const value = __ENV[name];
  if (!value) {
    throw new Error(`${name} environment variable is required`);
  }
  return value;
}

function requiredPositiveInteger(name) {
  const value = Number(requiredEnvironmentValue(name));
  if (!Number.isInteger(value) || value <= 0) {
    throw new Error(`${name} must be a positive integer`);
  }
  return value;
}

function positiveInteger(name, fallback) {
  const value = __ENV[name] ? Number(__ENV[name]) : fallback;
  if (!Number.isInteger(value) || value <= 0) {
    throw new Error(`${name} must be a positive integer`);
  }
  return value;
}

function nonNegativeInteger(name, fallback) {
  const value = __ENV[name] !== undefined ? Number(__ENV[name]) : fallback;
  if (!Number.isInteger(value) || value < 0) {
    throw new Error(`${name} must be a non-negative integer`);
  }
  return value;
}

function requiredRunId() {
  const runId = __ENV.RUN_ID;
  if (!runId) {
    throw new Error(
      "RUN_ID is required when k6 is called directly. Use test-script/run_registration_load_test.sh to generate it automatically",
    );
  }
  if (!/^[A-Za-z0-9-]+$/.test(runId)) {
    throw new Error("RUN_ID may contain only letters, numbers, and hyphens");
  }
  return runId;
}

function metricValue(data, metricName, valueName) {
  const metric = data.metrics[metricName];
  if (!metric || !metric.values || metric.values[valueName] === undefined) {
    return 0;
  }
  return metric.values[valueName];
}

function monitoredMetricValue(data, metricName, valueName) {
  return MONITOR_HIKARI ? metricValue(data, metricName, valueName) : null;
}

function displayMonitoredMetric(value) {
  return value === null ? "disabled" : value;
}

function actuatorMeasurement(response) {
  if (!response || response.status !== 200) {
    return 0;
  }
  try {
    const body = response.json();
    const measurement = body.measurements && body.measurements[0];
    return measurement ? Number(measurement.value) : 0;
  } catch (error) {
    return 0;
  }
}
