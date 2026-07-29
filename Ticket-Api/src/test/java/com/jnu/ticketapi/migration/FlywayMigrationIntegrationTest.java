package com.jnu.ticketapi.migration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import javax.persistence.EntityManagerFactory;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.flywaydb.core.api.output.MigrateResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Testcontainers(disabledWithoutDocker = true)
class FlywayMigrationIntegrationTest {

    @Container
    private static final MySQLContainer<?> MYSQL =
            new MySQLContainer<>(DockerImageName.parse("mysql:8.0.35"))
                    .withDatabaseName("migration_test")
                    .withUsername("migration")
                    .withPassword("migration")
                    .withTmpFs(Map.of("/var/lib/mysql", "rw"))
                    .withStartupTimeout(Duration.ofMinutes(5));

    @BeforeEach
    void cleanDatabase() {
        flyway(false, null).clean();
    }

    @Test
    void freshDatabaseAppliesEveryMigrationAndPassesHibernateValidation() throws SQLException {
        Flyway flyway = flyway(false, null);

        MigrateResult firstMigration = flyway.migrate();

        assertThat(firstMigration.migrationsExecuted).isEqualTo(4);
        assertThat(flyway.info().current().getVersion())
                .isEqualTo(MigrationVersion.fromVersion("4"));
        assertResultSchema();
        assertHibernateSchemaValidation();

        MigrateResult repeatedMigration = flyway.migrate();
        assertThat(repeatedMigration.migrationsExecuted).isZero();
    }

    @Test
    void existingDatabaseIsBaselinedAndOnlyLaterMigrationRuns() throws SQLException {
        Flyway legacySchema = flyway(false, MigrationVersion.fromVersion("1"));
        assertThat(legacySchema.migrate().migrationsExecuted).isEqualTo(1);
        insertHistoricalRegistrations();
        dropFlywayHistory();

        Flyway flyway = flyway(true, null);
        MigrateResult migration = flyway.migrate();

        assertThat(migration.migrationsExecuted).isEqualTo(3);
        assertThat(flyway.info().current().getVersion())
                .isEqualTo(MigrationVersion.fromVersion("4"));
        assertResultSchema();
        assertHistoricalRegistrationResults();
    }

    @Test
    void manuallyUpdatedExistingDatabaseCanAdoptFlywayWithoutDuplicateDdl() throws SQLException {
        Flyway legacySchema = flyway(false, MigrationVersion.fromVersion("1"));
        legacySchema.migrate();
        applyExistingManualResultSchema();
        insertExistingExhaustedOutbox();
        dropFlywayHistory();

        Flyway flyway = flyway(true, null);
        MigrateResult migration = flyway.migrate();

        assertThat(migration.migrationsExecuted).isEqualTo(3);
        assertResultSchema();
        assertExistingExhaustedOutboxWasBackfilled();
        assertThat(flyway.migrate().migrationsExecuted).isZero();
    }

    @Test
    void completedSectorIsPreservedWhileHistoricalSectorIsBackfilled() throws SQLException {
        flyway(false, MigrationVersion.fromVersion("3")).migrate();
        insertHistoricalRegistrations();
        updateResult(7L, 1, "합격", -2);
        updateResult(8L, 2, "예비", 1);

        MigrateResult migration = flyway(false, null).migrate();

        assertThat(migration.migrationsExecuted).isEqualTo(1);
        assertHistoricalRegistrationResults();
    }

    @Test
    void missingSavedAtBlocksBackfillBeforeAnyResultIsChanged() throws SQLException {
        flyway(false, MigrationVersion.fromVersion("3")).migrate();
        insertHistoricalRegistrations();
        execute("UPDATE registration_tb SET is_saved = b'1' WHERE id = 5");

        assertThatThrownBy(() -> flyway(false, null).migrate())
                .hasStackTraceContaining("__flyway_blocked_registration_missing_saved_at");
        assertRegistrationResultIsNull(1L);
        assertEmailOutboxIsEmpty();
    }

    @Test
    void mixedResultStateInOneSectorBlocksBackfillBeforeMutation() throws SQLException {
        flyway(false, MigrationVersion.fromVersion("3")).migrate();
        insertHistoricalRegistrations();
        updateResult(1L, 1, "합격", -2);

        assertThatThrownBy(() -> flyway(false, null).migrate())
                .hasStackTraceContaining("__flyway_blocked_registration_mixed_sector_result");
        assertRegistrationResultIsNull(2L);
        assertEmailOutboxIsEmpty();
    }

    @Test
    void activeRegistrationInInvisibleSectorBlocksBackfillBeforeMutation() throws SQLException {
        flyway(false, MigrationVersion.fromVersion("3")).migrate();
        insertHistoricalRegistrations();
        execute("UPDATE sector SET event_id = NULL WHERE sector_id = 100");

        assertThatThrownBy(() -> flyway(false, null).migrate())
                .hasStackTraceContaining("__flyway_blocked_registration_invalid_sector_reference");
        assertRegistrationResultIsNull(1L);
        assertEmailOutboxIsEmpty();
    }

    @Test
    void activeRegistrationWithoutSectorBlocksBackfillBeforeMutation() throws SQLException {
        flyway(false, MigrationVersion.fromVersion("3")).migrate();
        insertHistoricalRegistrations();
        orphanRegistrationFromSector(1L, 999L);

        assertThatThrownBy(() -> flyway(false, null).migrate())
                .hasStackTraceContaining("__flyway_blocked_registration_invalid_sector_reference");
        assertRegistrationResultIsNull(2L);
        assertEmailOutboxIsEmpty();
    }

    private Flyway flyway(boolean baselineOnMigrate, MigrationVersion target) {
        var configuration =
                Flyway.configure()
                        .dataSource(MYSQL.getJdbcUrl(), MYSQL.getUsername(), MYSQL.getPassword())
                        .locations("classpath:db/migration")
                        .baselineOnMigrate(baselineOnMigrate)
                        .baselineVersion(MigrationVersion.fromVersion("1"))
                        .cleanDisabled(false);
        if (target != null) {
            configuration.target(target);
        }
        return configuration.load();
    }

    private void assertResultSchema() throws SQLException {
        try (Connection connection = connection()) {
            assertThat(hasColumn(connection, "registration_tb", "position")).isTrue();
            assertThat(hasColumn(connection, "registration_tb", "result_status")).isTrue();
            assertThat(hasColumn(connection, "registration_tb", "sequence")).isTrue();
            assertThat(hasTable(connection, "email_outbox")).isTrue();
            assertThat(hasColumn(connection, "email_outbox", "failed_at")).isTrue();
            assertThat(hasColumn(connection, "email_outbox", "last_error")).isTrue();
            assertThat(hasIndex(connection, "email_outbox", "uk_email_outbox_registration_id"))
                    .isTrue();
            assertThat(hasIndex(connection, "email_outbox", "idx_email_outbox_pending")).isTrue();
            assertThat(hasIndex(connection, "email_outbox", "idx_email_outbox_failed")).isTrue();
            assertThat(hasForeignKey(connection, "email_outbox", "fk_email_outbox_registration"))
                    .isTrue();
        }
    }

    private void assertHibernateSchemaValidation() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName(MYSQL.getDriverClassName());
        dataSource.setUrl(MYSQL.getJdbcUrl());
        dataSource.setUsername(MYSQL.getUsername());
        dataSource.setPassword(MYSQL.getPassword());

        Map<String, Object> properties = new HashMap<>();
        properties.put("hibernate.hbm2ddl.auto", "validate");
        properties.put("hibernate.dialect", "org.hibernate.dialect.MySQL8Dialect");
        properties.put(
                "hibernate.physical_naming_strategy",
                "org.springframework.boot.orm.jpa.hibernate.SpringPhysicalNamingStrategy");
        properties.put(
                "hibernate.implicit_naming_strategy",
                "org.springframework.boot.orm.jpa.hibernate.SpringImplicitNamingStrategy");

        LocalContainerEntityManagerFactoryBean factory =
                new LocalContainerEntityManagerFactoryBean();
        factory.setPersistenceUnitName("flyway-migration-validation");
        factory.setDataSource(dataSource);
        factory.setPackagesToScan("com.jnu.ticketdomain");
        factory.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
        factory.setJpaPropertyMap(properties);

        try {
            factory.afterPropertiesSet();
            EntityManagerFactory entityManagerFactory = factory.getObject();
            assertThat(entityManagerFactory).isNotNull();
        } finally {
            factory.destroy();
        }
    }

    private void applyExistingManualResultSchema() throws SQLException {
        try (Connection connection = connection();
                Statement statement = connection.createStatement()) {
            statement.execute(
                    "ALTER TABLE registration_tb ADD COLUMN position INT NULL AFTER saved_at");
            statement.execute(
                    "ALTER TABLE registration_tb ADD COLUMN result_status VARCHAR(255) NULL AFTER position");
            statement.execute(
                    "ALTER TABLE registration_tb ADD COLUMN sequence INT NULL AFTER result_status");
            statement.execute(
                    "CREATE TABLE email_outbox ("
                            + "id BIGINT AUTO_INCREMENT PRIMARY KEY, "
                            + "event_id BIGINT NOT NULL, "
                            + "registration_id BIGINT NOT NULL, "
                            + "email VARCHAR(255) NOT NULL, "
                            + "name VARCHAR(255) NOT NULL, "
                            + "result_status VARCHAR(255) NOT NULL, "
                            + "sequence INT NOT NULL, "
                            + "created_at DATETIME(6) NOT NULL, "
                            + "processing_at DATETIME(6) NULL, "
                            + "sent_at DATETIME(6) NULL, "
                            + "retry_count INT NOT NULL, "
                            + "CONSTRAINT uk_email_outbox_registration_id UNIQUE (registration_id), "
                            + "CONSTRAINT FK_email_outbox_registration FOREIGN KEY (registration_id) "
                            + "REFERENCES registration_tb (id))");
        }
    }

    private void dropFlywayHistory() throws SQLException {
        try (Connection connection = connection();
                Statement statement = connection.createStatement()) {
            statement.execute("DROP TABLE flyway_schema_history");
        }
    }

    private void insertExistingExhaustedOutbox() throws SQLException {
        try (Connection connection = connection();
                Statement statement = connection.createStatement()) {
            statement.execute("SET FOREIGN_KEY_CHECKS = 0");
            statement.execute(
                    "INSERT INTO email_outbox "
                            + "(id, event_id, registration_id, email, name, result_status, sequence, "
                            + "created_at, processing_at, sent_at, retry_count) VALUES "
                            + "(1, 10, 999, 'failed@jnu.ac.kr', '학생', '불합격', -1, "
                            + "'2026-07-28 10:00:00', '2026-07-28 11:00:00', NULL, 10)");
            statement.execute("SET FOREIGN_KEY_CHECKS = 1");
        }
    }

    private void insertHistoricalRegistrations() throws SQLException {
        try (Connection connection = connection();
                Statement statement = connection.createStatement()) {
            statement.execute(
                    "INSERT INTO event "
                            + "(event_id, event_status, is_deleted, publish, title) VALUES "
                            + "(10, 'CLOSED', b'0', b'1', 'historical event')");
            statement.execute(
                    "INSERT INTO sector "
                            + "(sector_id, init_reserve, init_sector_capacity, is_deleted, "
                            + "issue_amount, name, remaining_amount, reserve, sector_capacity, "
                            + "sector_number, event_id) VALUES "
                            + "(100, 1, 2, b'0', 3, 'engineering', 0, 0, 0, '1', 10), "
                            + "(101, 1, 1, b'0', 2, 'humanities', 0, 0, 0, '2', 10)");
            statement.execute(
                    "INSERT INTO user_tb "
                            + "(user_id, email, email_confirmed, pwd, sequence, status, role) "
                            + "VALUES (1, 'current@jnu.ac.kr', b'1', 'encoded', 99, '예비', 'USER')");
            statement.execute(
                    "INSERT INTO registration_tb "
                            + "(id, affiliation, car_num, created_at, email, is_deleted, is_light, "
                            + "is_saved, name, phone_num, saved_at, student_num, sector_id, "
                            + "user_id, department, event_id) VALUES "
                            + "(1, 'college', 'car-1', '2025-01-01 09:00:00', 'one@jnu.ac.kr', "
                            + "b'0', b'0', b'1', 'one', '010-0000-0001', 1000, '20250001', "
                            + "100, 1, 'department', NULL), "
                            + "(2, 'college', 'car-2', '2025-01-01 09:00:00', 'two@jnu.ac.kr', "
                            + "b'0', b'0', b'1', 'two', '010-0000-0002', 2000, '20250002', "
                            + "100, NULL, 'department', NULL), "
                            + "(3, 'college', 'car-3', '2025-01-01 09:00:00', 'three@jnu.ac.kr', "
                            + "b'0', b'0', b'1', 'three', '010-0000-0003', 3000, '20250003', "
                            + "100, NULL, 'department', NULL), "
                            + "(4, 'college', 'car-4', '2025-01-01 09:00:00', 'four@jnu.ac.kr', "
                            + "b'0', b'0', b'1', 'four', '010-0000-0004', 4000, '20250004', "
                            + "100, NULL, 'department', NULL), "
                            + "(5, 'college', 'car-5', '2025-01-01 09:00:00', 'five@jnu.ac.kr', "
                            + "b'0', b'0', b'0', 'five', '010-0000-0005', NULL, '20250005', "
                            + "100, NULL, 'department', NULL), "
                            + "(6, 'college', 'car-6', '2025-01-01 09:00:00', 'six@jnu.ac.kr', "
                            + "b'1', b'0', b'1', 'six', '010-0000-0006', 500, '20250006', "
                            + "100, NULL, 'department', NULL), "
                            + "(7, 'college', 'car-7', '2025-01-01 09:00:00', 'seven@jnu.ac.kr', "
                            + "b'0', b'0', b'1', 'seven', '010-0000-0007', 1500, '20250007', "
                            + "101, NULL, 'department', NULL), "
                            + "(8, 'college', 'car-8', '2025-01-01 09:00:00', 'eight@jnu.ac.kr', "
                            + "b'0', b'0', b'1', 'eight', '010-0000-0008', 2500, '20250008', "
                            + "101, NULL, 'department', NULL)");
        }
    }

    private void assertHistoricalRegistrationResults() throws SQLException {
        assertRegistrationResult(1L, 1, "합격", -2);
        assertRegistrationResult(2L, 2, "합격", -2);
        assertRegistrationResult(3L, 3, "예비", 1);
        assertRegistrationResult(4L, 4, "불합격", -1);
        assertRegistrationResultIsNull(5L);
        assertRegistrationResultIsNull(6L);
        assertRegistrationResult(7L, 1, "합격", -2);
        assertRegistrationResult(8L, 2, "예비", 1);
        assertEmailOutboxIsEmpty();

        try (Connection connection = connection();
                Statement statement = connection.createStatement();
                ResultSet result =
                        statement.executeQuery(
                                "SELECT status, sequence FROM user_tb WHERE user_id = 1")) {
            assertThat(result.next()).isTrue();
            assertThat(result.getString("status")).isEqualTo("예비");
            assertThat(result.getInt("sequence")).isEqualTo(99);
        }
    }

    private void assertRegistrationResult(
            Long registrationId, Integer position, String resultStatus, Integer sequence)
            throws SQLException {
        try (Connection connection = connection();
                Statement statement = connection.createStatement();
                ResultSet result =
                        statement.executeQuery(
                                "SELECT position, result_status, sequence FROM registration_tb "
                                        + "WHERE id = "
                                        + registrationId)) {
            assertThat(result.next()).isTrue();
            assertThat(result.getObject("position", Integer.class)).isEqualTo(position);
            assertThat(result.getString("result_status")).isEqualTo(resultStatus);
            assertThat(result.getObject("sequence", Integer.class)).isEqualTo(sequence);
        }
    }

    private void assertRegistrationResultIsNull(Long registrationId) throws SQLException {
        assertRegistrationResult(registrationId, null, null, null);
    }

    private void assertEmailOutboxIsEmpty() throws SQLException {
        try (Connection connection = connection();
                Statement statement = connection.createStatement();
                ResultSet result = statement.executeQuery("SELECT COUNT(*) FROM email_outbox")) {
            assertThat(result.next()).isTrue();
            assertThat(result.getInt(1)).isZero();
        }
    }

    private void updateResult(
            Long registrationId, Integer position, String resultStatus, Integer sequence)
            throws SQLException {
        execute(
                "UPDATE registration_tb SET position = "
                        + position
                        + ", result_status = '"
                        + resultStatus
                        + "', sequence = "
                        + sequence
                        + " WHERE id = "
                        + registrationId);
    }

    private void execute(String sql) throws SQLException {
        try (Connection connection = connection();
                Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }

    private void orphanRegistrationFromSector(Long registrationId, Long sectorId)
            throws SQLException {
        try (Connection connection = connection();
                Statement statement = connection.createStatement()) {
            statement.execute("SET FOREIGN_KEY_CHECKS = 0");
            try {
                statement.execute(
                        "UPDATE registration_tb SET sector_id = "
                                + sectorId
                                + " WHERE id = "
                                + registrationId);
            } finally {
                statement.execute("SET FOREIGN_KEY_CHECKS = 1");
            }
        }
    }

    private void assertExistingExhaustedOutboxWasBackfilled() throws SQLException {
        try (Connection connection = connection();
                Statement statement = connection.createStatement();
                ResultSet result =
                        statement.executeQuery(
                                "SELECT failed_at, processing_at, last_error "
                                        + "FROM email_outbox WHERE id = 1")) {
            assertThat(result.next()).isTrue();
            assertThat(result.getTimestamp("failed_at")).isNotNull();
            assertThat(result.getTimestamp("processing_at")).isNull();
            assertThat(result.getString("last_error")).isNotBlank();
        }
    }

    private Connection connection() throws SQLException {
        return DriverManager.getConnection(
                MYSQL.getJdbcUrl(), MYSQL.getUsername(), MYSQL.getPassword());
    }

    private boolean hasTable(Connection connection, String tableName) throws SQLException {
        DatabaseMetaData metadata = connection.getMetaData();
        try (ResultSet tables =
                metadata.getTables(
                        connection.getCatalog(), null, tableName, new String[] {"TABLE"})) {
            return tables.next();
        }
    }

    private boolean hasColumn(Connection connection, String tableName, String columnName)
            throws SQLException {
        DatabaseMetaData metadata = connection.getMetaData();
        try (ResultSet columns =
                metadata.getColumns(connection.getCatalog(), null, tableName, columnName)) {
            return columns.next();
        }
    }

    private boolean hasIndex(Connection connection, String tableName, String indexName)
            throws SQLException {
        DatabaseMetaData metadata = connection.getMetaData();
        try (ResultSet indexes =
                metadata.getIndexInfo(connection.getCatalog(), null, tableName, false, false)) {
            while (indexes.next()) {
                if (indexName.equalsIgnoreCase(indexes.getString("INDEX_NAME"))) {
                    return true;
                }
            }
            return false;
        }
    }

    private boolean hasForeignKey(Connection connection, String tableName, String constraintName)
            throws SQLException {
        DatabaseMetaData metadata = connection.getMetaData();
        try (ResultSet foreignKeys =
                metadata.getImportedKeys(connection.getCatalog(), null, tableName)) {
            while (foreignKeys.next()) {
                if (constraintName.equalsIgnoreCase(foreignKeys.getString("FK_NAME"))) {
                    return true;
                }
            }
            return false;
        }
    }
}
