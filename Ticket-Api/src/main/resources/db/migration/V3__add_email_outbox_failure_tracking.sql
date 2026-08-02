SET @schema_name = DATABASE();

SET @migration_statement = IF(
    EXISTS(
        SELECT 1
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = @schema_name
          AND TABLE_NAME = 'email_outbox'
          AND COLUMN_NAME = 'failed_at'
    ),
    'SELECT 1',
    'ALTER TABLE email_outbox ADD COLUMN failed_at DATETIME(6) NULL AFTER sent_at'
);
PREPARE migration_statement FROM @migration_statement;
EXECUTE migration_statement;
DEALLOCATE PREPARE migration_statement;

SET @migration_statement = IF(
    EXISTS(
        SELECT 1
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = @schema_name
          AND TABLE_NAME = 'email_outbox'
          AND COLUMN_NAME = 'last_error'
    ),
    'SELECT 1',
    'ALTER TABLE email_outbox ADD COLUMN last_error VARCHAR(1000) NULL AFTER failed_at'
);
PREPARE migration_statement FROM @migration_statement;
EXECUTE migration_statement;
DEALLOCATE PREPARE migration_statement;

UPDATE email_outbox
SET failed_at = COALESCE(processing_at, created_at),
    processing_at = NULL,
    last_error = COALESCE(last_error, 'Flyway 도입 이전 최대 재시도 횟수 도달')
WHERE sent_at IS NULL
  AND failed_at IS NULL
  AND retry_count >= 10;

DROP INDEX idx_email_outbox_pending ON email_outbox;
CREATE INDEX idx_email_outbox_pending
    ON email_outbox (sent_at, failed_at, retry_count, processing_at, id);

SET @migration_statement = IF(
    EXISTS(
        SELECT 1
        FROM information_schema.STATISTICS
        WHERE TABLE_SCHEMA = @schema_name
          AND TABLE_NAME = 'email_outbox'
          AND INDEX_NAME = 'idx_email_outbox_failed'
    ),
    'SELECT 1',
    'CREATE INDEX idx_email_outbox_failed ON email_outbox (event_id, failed_at, id)'
);
PREPARE migration_statement FROM @migration_statement;
EXECUTE migration_statement;
DEALLOCATE PREPARE migration_statement;
