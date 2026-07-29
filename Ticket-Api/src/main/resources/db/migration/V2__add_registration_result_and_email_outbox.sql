SET @schema_name = DATABASE();

SET @migration_statement = IF(
    EXISTS(
        SELECT 1
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = @schema_name
          AND TABLE_NAME = 'registration_tb'
          AND COLUMN_NAME = 'position'
    ),
    'SELECT 1',
    'ALTER TABLE registration_tb ADD COLUMN position INT NULL AFTER saved_at'
);
PREPARE migration_statement FROM @migration_statement;
EXECUTE migration_statement;
DEALLOCATE PREPARE migration_statement;

SET @migration_statement = IF(
    EXISTS(
        SELECT 1
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = @schema_name
          AND TABLE_NAME = 'registration_tb'
          AND COLUMN_NAME = 'result_status'
    ),
    'SELECT 1',
    'ALTER TABLE registration_tb ADD COLUMN result_status VARCHAR(255) NULL AFTER position'
);
PREPARE migration_statement FROM @migration_statement;
EXECUTE migration_statement;
DEALLOCATE PREPARE migration_statement;

SET @migration_statement = IF(
    EXISTS(
        SELECT 1
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = @schema_name
          AND TABLE_NAME = 'registration_tb'
          AND COLUMN_NAME = 'sequence'
    ),
    'SELECT 1',
    'ALTER TABLE registration_tb ADD COLUMN sequence INT NULL AFTER result_status'
);
PREPARE migration_statement FROM @migration_statement;
EXECUTE migration_statement;
DEALLOCATE PREPARE migration_statement;

CREATE TABLE IF NOT EXISTS email_outbox
(
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    event_id        BIGINT       NOT NULL,
    registration_id BIGINT       NOT NULL,
    email           VARCHAR(255) NOT NULL,
    name            VARCHAR(255) NOT NULL,
    result_status   VARCHAR(255) NOT NULL,
    sequence        INT          NOT NULL,
    created_at      DATETIME(6)  NOT NULL,
    processing_at   DATETIME(6)  NULL,
    sent_at         DATETIME(6)  NULL,
    retry_count     INT          NOT NULL,
    CONSTRAINT uk_email_outbox_registration_id UNIQUE (registration_id),
    CONSTRAINT fk_email_outbox_registration
        FOREIGN KEY (registration_id) REFERENCES registration_tb (id),
    INDEX idx_email_outbox_pending (sent_at, retry_count, processing_at, id)
);

SET @migration_statement = IF(
    EXISTS(
        SELECT 1
        FROM information_schema.STATISTICS
        WHERE TABLE_SCHEMA = @schema_name
          AND TABLE_NAME = 'email_outbox'
          AND INDEX_NAME = 'uk_email_outbox_registration_id'
    ),
    'SELECT 1',
    'ALTER TABLE email_outbox ADD CONSTRAINT uk_email_outbox_registration_id UNIQUE (registration_id)'
);
PREPARE migration_statement FROM @migration_statement;
EXECUTE migration_statement;
DEALLOCATE PREPARE migration_statement;

SET @migration_statement = IF(
    EXISTS(
        SELECT 1
        FROM information_schema.REFERENTIAL_CONSTRAINTS
        WHERE CONSTRAINT_SCHEMA = @schema_name
          AND TABLE_NAME = 'email_outbox'
          AND CONSTRAINT_NAME = 'fk_email_outbox_registration'
    ),
    'SELECT 1',
    'ALTER TABLE email_outbox ADD CONSTRAINT fk_email_outbox_registration FOREIGN KEY (registration_id) REFERENCES registration_tb (id)'
);
PREPARE migration_statement FROM @migration_statement;
EXECUTE migration_statement;
DEALLOCATE PREPARE migration_statement;

SET @migration_statement = IF(
    EXISTS(
        SELECT 1
        FROM information_schema.STATISTICS
        WHERE TABLE_SCHEMA = @schema_name
          AND TABLE_NAME = 'email_outbox'
          AND INDEX_NAME = 'idx_email_outbox_pending'
    ),
    'SELECT 1',
    'CREATE INDEX idx_email_outbox_pending ON email_outbox (sent_at, retry_count, processing_at, id)'
);
PREPARE migration_statement FROM @migration_statement;
EXECUTE migration_statement;
DEALLOCATE PREPARE migration_statement;
