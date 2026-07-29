package com.jnu.ticketapi.migration;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
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
                    .withPassword("migration");

    @BeforeEach
    void cleanDatabase() {
        flyway(false, null).clean();
    }

    @Test
    void freshDatabaseAppliesEveryMigrationAndPassesHibernateValidation() throws SQLException {
        Flyway flyway = flyway(false, null);

        MigrateResult firstMigration = flyway.migrate();

        assertThat(firstMigration.migrationsExecuted).isEqualTo(2);
        assertThat(flyway.info().current().getVersion())
                .isEqualTo(MigrationVersion.fromVersion("2"));
        assertResultSchema();
        assertHibernateSchemaValidation();

        MigrateResult repeatedMigration = flyway.migrate();
        assertThat(repeatedMigration.migrationsExecuted).isZero();
    }

    @Test
    void existingDatabaseIsBaselinedAndOnlyLaterMigrationRuns() throws SQLException {
        Flyway legacySchema = flyway(false, MigrationVersion.fromVersion("1"));
        assertThat(legacySchema.migrate().migrationsExecuted).isEqualTo(1);
        dropFlywayHistory();

        Flyway flyway = flyway(true, null);
        MigrateResult migration = flyway.migrate();

        assertThat(migration.migrationsExecuted).isEqualTo(1);
        assertThat(flyway.info().current().getVersion())
                .isEqualTo(MigrationVersion.fromVersion("2"));
        assertResultSchema();
    }

    @Test
    void manuallyUpdatedExistingDatabaseCanAdoptFlywayWithoutDuplicateDdl() throws SQLException {
        Flyway legacySchema = flyway(false, MigrationVersion.fromVersion("1"));
        legacySchema.migrate();
        applyExistingManualResultSchema();
        dropFlywayHistory();

        Flyway flyway = flyway(true, null);
        MigrateResult migration = flyway.migrate();

        assertThat(migration.migrationsExecuted).isEqualTo(1);
        assertResultSchema();
        assertThat(flyway.migrate().migrationsExecuted).isZero();
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
            assertThat(hasIndex(connection, "email_outbox", "uk_email_outbox_registration_id"))
                    .isTrue();
            assertThat(hasIndex(connection, "email_outbox", "idx_email_outbox_pending")).isTrue();
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
