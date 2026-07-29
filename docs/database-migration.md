# Flyway 데이터베이스 마이그레이션

## 적용 원칙

- 애플리케이션, Spring Batch, Quartz 테이블은 `Ticket-Api/src/main/resources/db/migration`에서 관리합니다.
- Hibernate는 스키마를 변경하지 않고 `ddl-auto: validate`로 엔티티와 실제 스키마의 일치 여부만 확인합니다.
- 적용이 끝난 migration 파일은 수정하지 않습니다. 이후 변경은 다음 버전 파일로 추가합니다.
- 테스트 프로파일은 기존 H2 `create-drop` 흐름을 유지하며 Flyway를 실행하지 않습니다.

## 신규 로컬 DB

MySQL 컨테이너는 `MYSQL_DATABASE`로 빈 데이터베이스만 생성합니다. 애플리케이션 시작 시 Flyway가 다음 순서로 스키마를 구성합니다.

1. `V1__baseline_schema.sql`: 기존 애플리케이션, Spring Batch, Quartz 스키마
2. `V2__add_registration_result_and_email_outbox.sql`: 신청 결과 컬럼과 메일 Outbox
3. `V3__add_email_outbox_failure_tracking.sql`: Outbox 최종 실패 상태와 조회 인덱스
4. `V4__backfill_historical_registration_results.sql`: 과거 저장 완료 신청의 순번과 결과 복원

로컬 애플리케이션의 DB 이름은 `DB_NAME`, `MYSQL_DATABASE`, 기본값 `jnu-parking` 순서로 결정됩니다.

## 기존 DB 최초 편입

운영 환경은 기본적으로 `FLYWAY_BASELINE_ON_MIGRATE=false`입니다. 기존 테이블이 있지만 `flyway_schema_history`가 없는 DB에 실수로 baseline을 기록하지 않도록 fail-closed로 둔 설정입니다.

최초 편입 시에만 다음 순서를 따릅니다.

1. 신청 이벤트가 열려 있지 않은 시간에 운영 DB 스냅샷 또는 복구 가능한 백업을 생성합니다.
2. migration 계정의 `ALTER`, `CREATE`, `INDEX`, `REFERENCES`, `CREATE TEMPORARY TABLES` 권한을 확인합니다.
3. 기존 스키마가 V1 기준 테이블을 보유했는지 확인하고 아래 V4 사전 점검 SQL을 실행합니다.
4. 운영 서버 환경에 최초 1회만 `FLYWAY_BASELINE_ON_MIGRATE=true`를 주입해 애플리케이션을 시작합니다.
5. 현재 운영 배포 workflow가 `/api/actuator/health`의 `UP`을 확인했는지 확인합니다. 제한 시간 안에 기동하지 못하거나 컨테이너가 종료되면 배포는 실패해야 합니다.
6. `flyway_schema_history`에 version 1 baseline과 version 2, 3, 4 success가 기록됐는지 확인합니다.
7. `registration_tb` 결과 컬럼, `email_outbox`, Outbox 인덱스와 과거 결과 검증 쿼리를 확인합니다.
8. 이후 배포부터 `FLYWAY_BASELINE_ON_MIGRATE=false`로 되돌립니다.

V2는 기존 DB에 신청 결과 컬럼이나 Outbox가 이미 수동 반영된 경우 해당 DDL을 건너뛰고 누락된 인덱스와 제약만 보완합니다.

## V4 과거 신청 결과 복원

V4는 `is_saved = true`, `is_deleted = false`인 신청만 대상으로 합니다. 결과 세 필드가 모두 비어 있는 구간은 `sector_id`별 `saved_at ASC, id ASC` 순서로 `position`을 계산하고 다음 규칙을 적용합니다.

- `position <= init_sector_capacity`: 합격, `sequence = -2`
- `position <= issue_amount`: 예비, `sequence = position - init_sector_capacity`
- 나머지: 불합격, `sequence = -1`

이미 한 구간의 결과가 모두 완성돼 있으면 기존 값을 보존하고 정합성만 검증합니다. 다음 상태는 자동으로 추정하지 않고 애플리케이션 시작을 중단합니다.

- 저장 완료 신청의 `saved_at` 누락
- `issue_amount < init_sector_capacity`인 구간
- 활성 저장 신청이 이벤트 없는 구간 또는 삭제된 구간을 참조하는 상태
- `position`, `result_status`, `sequence`가 일부만 채워진 신청
- 같은 구간에 확정 결과와 미확정 결과가 혼재
- 기존 순번의 중복·누락 또는 순번과 결과의 불일치

V4는 `user_tb.status`, `user_tb.sequence`, 삭제된 신청, 임시 저장 신청을 변경하지 않습니다. 또한 `email_outbox`를 생성하지 않으므로 배포만으로 과거 사용자에게 메일이 발송되지 않습니다.

### 배포 전 점검

```sql
SELECT event_id, title, event_status
FROM event
WHERE event_status IN ('OPEN', 'CALCULATING');

SELECT COUNT(*) AS missing_saved_at_count
FROM registration_tb
WHERE is_saved = b'1'
  AND is_deleted = b'0'
  AND saved_at IS NULL;

SELECT sector_id, init_sector_capacity, issue_amount
FROM sector
WHERE issue_amount < init_sector_capacity
  AND EXISTS (
      SELECT 1
      FROM registration_tb
      WHERE registration_tb.sector_id = sector.sector_id
        AND registration_tb.is_saved = b'1'
        AND registration_tb.is_deleted = b'0'
  );

SELECT registration.id, registration.sector_id
FROM registration_tb registration
INNER JOIN sector ON sector.sector_id = registration.sector_id
WHERE registration.is_saved = b'1'
  AND registration.is_deleted = b'0'
  AND (sector.event_id IS NULL OR sector.is_deleted IS NULL OR sector.is_deleted <> b'0');
```

첫 번째, 세 번째, 네 번째 쿼리는 행이 없어야 하며, `missing_saved_at_count`는 `0`이어야 합니다. 결과 컬럼을 이미 수동 추가한 DB라면 일부 결과나 한 구간 내 결과 혼재가 없는지도 별도로 확인합니다.

### 배포 후 점검

```sql
SELECT installed_rank, version, description, success
FROM flyway_schema_history
ORDER BY installed_rank;

SELECT COUNT(*) AS missing_result_count
FROM registration_tb
WHERE is_saved = b'1'
  AND is_deleted = b'0'
  AND (position IS NULL OR result_status IS NULL OR sequence IS NULL);

SELECT sector_id,
       COUNT(*) AS registration_count,
       COUNT(DISTINCT position) AS distinct_position_count,
       MIN(position) AS min_position,
       MAX(position) AS max_position
FROM registration_tb
WHERE is_saved = b'1'
  AND is_deleted = b'0'
GROUP BY sector_id
HAVING distinct_position_count <> registration_count
    OR min_position <> 1
    OR max_position <> registration_count;
```

version 4가 `success = 1`, 누락 결과가 `0`, 마지막 쿼리가 0행인지 확인합니다.

## 실패와 복구

- migration이 실패하면 Hibernate 초기화 전에 애플리케이션 시작이 중단됩니다.
- 운영 DB를 자동으로 `clean`하거나 기존 migration을 되돌리지 않습니다.
- V4 차단 오류가 발생하면 애플리케이션을 반복 재시작하지 말고 오류에 표시된 `__flyway_blocked_registration_*` 원인과 원본 데이터를 먼저 확인합니다.
- MySQL migration은 전체가 하나의 트랜잭션으로 되돌아가지 않습니다. V4 후검증에서 실패했다면 백업과 실제 변경 행을 대조한 뒤 복구 절차를 결정합니다.
- 원인을 수정한 뒤 실패한 version 4 이력을 정리하거나 `flyway repair`를 수행하는 작업은 DB 백업과 데이터 상태를 확인한 운영 담당자만 진행합니다.
- 적용이 끝난 migration 파일을 수정하지 않고 새 forward migration을 추가하는 것이 기본 복구 방식입니다.
- 애플리케이션만 이전 버전으로 되돌려야 할 때는 이전 버전이 추가 컬럼과 테이블을 무시할 수 있는지 먼저 확인합니다.

Flyway는 애플리케이션 라이브러리와 기존 MySQL을 사용하므로 별도 AWS 서비스나 상시 인프라 비용을 추가하지 않습니다.
