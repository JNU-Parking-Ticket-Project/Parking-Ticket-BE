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

로컬 애플리케이션의 DB 이름은 `DB_NAME`, `MYSQL_DATABASE`, 기본값 `jnu-parking` 순서로 결정됩니다.

## 기존 DB 최초 편입

운영 환경은 기본적으로 `FLYWAY_BASELINE_ON_MIGRATE=false`입니다. 기존 테이블이 있지만 `flyway_schema_history`가 없는 DB에 실수로 baseline을 기록하지 않도록 fail-closed로 둔 설정입니다.

최초 편입 시에만 다음 순서를 따릅니다.

1. 현재 백업 정책과 migration 계정의 `ALTER`, `CREATE`, `INDEX`, `REFERENCES` 권한을 확인합니다.
2. 기존 스키마가 V1 기준 테이블을 보유했는지 확인합니다.
3. 한 서버에 `FLYWAY_BASELINE_ON_MIGRATE=true`를 주입해 애플리케이션을 시작합니다.
4. `flyway_schema_history`에 version 1 baseline과 version 2 success가 기록됐는지 확인합니다.
5. `registration_tb` 결과 컬럼, `email_outbox`, `idx_email_outbox_pending` 생성을 확인합니다.
6. 이후 배포부터 `FLYWAY_BASELINE_ON_MIGRATE=false`로 되돌립니다.

V2는 기존 DB에 신청 결과 컬럼이나 Outbox가 이미 수동 반영된 경우 해당 DDL을 건너뛰고 누락된 인덱스와 제약만 보완합니다.

## 실패와 복구

- migration이 실패하면 Hibernate 초기화 전에 애플리케이션 시작이 중단됩니다.
- 운영 DB를 자동으로 `clean`하거나 기존 migration을 되돌리지 않습니다.
- 실패 원인을 수정한 새 forward migration을 추가하는 것이 기본 복구 방식입니다.
- 애플리케이션만 이전 버전으로 되돌려야 할 때는 이전 버전이 추가 컬럼과 테이블을 무시할 수 있는지 먼저 확인합니다.

Flyway는 애플리케이션 라이브러리와 기존 MySQL을 사용하므로 별도 AWS 서비스나 상시 인프라 비용을 추가하지 않습니다.
