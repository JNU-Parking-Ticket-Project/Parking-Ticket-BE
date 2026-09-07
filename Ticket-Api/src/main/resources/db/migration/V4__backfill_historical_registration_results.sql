SET @missing_saved_at_count = (
    SELECT COUNT(*)
    FROM registration_tb
    WHERE is_saved = b'1'
      AND is_deleted = b'0'
      AND saved_at IS NULL
);
SET @validation_statement = IF(
    @missing_saved_at_count = 0,
    'SELECT 1',
    'SELECT 1 FROM __flyway_blocked_registration_missing_saved_at'
);
PREPARE validation_statement FROM @validation_statement;
EXECUTE validation_statement;
DEALLOCATE PREPARE validation_statement;

SET @invalid_capacity_count = (
    SELECT COUNT(*)
    FROM sector
    WHERE issue_amount < init_sector_capacity
      AND EXISTS (
          SELECT 1
          FROM registration_tb registration
          WHERE registration.sector_id = sector.sector_id
            AND registration.is_saved = b'1'
            AND registration.is_deleted = b'0'
      )
);
SET @validation_statement = IF(
    @invalid_capacity_count = 0,
    'SELECT 1',
    'SELECT 1 FROM __flyway_blocked_registration_invalid_sector_capacity'
);
PREPARE validation_statement FROM @validation_statement;
EXECUTE validation_statement;
DEALLOCATE PREPARE validation_statement;

SET @invalid_sector_reference_count = (
    SELECT COUNT(*)
    FROM registration_tb registration
    LEFT JOIN sector ON sector.sector_id = registration.sector_id
    WHERE registration.is_saved = b'1'
      AND registration.is_deleted = b'0'
      AND (
          sector.sector_id IS NULL
          OR sector.event_id IS NULL
          OR sector.is_deleted IS NULL
          OR sector.is_deleted <> b'0'
      )
);
SET @validation_statement = IF(
    @invalid_sector_reference_count = 0,
    'SELECT 1',
    'SELECT 1 FROM __flyway_blocked_registration_invalid_sector_reference'
);
PREPARE validation_statement FROM @validation_statement;
EXECUTE validation_statement;
DEALLOCATE PREPARE validation_statement;

DROP TEMPORARY TABLE IF EXISTS registration_result_backfill;
CREATE TEMPORARY TABLE registration_result_backfill
(
    registration_id BIGINT       NOT NULL PRIMARY KEY,
    position        INT          NOT NULL,
    result_status   VARCHAR(255) NOT NULL,
    sequence        INT          NOT NULL
);

INSERT INTO registration_result_backfill
    (registration_id, position, result_status, sequence)
SELECT ranked.registration_id,
       ranked.position,
       CASE
           WHEN ranked.position <= ranked.init_sector_capacity THEN '합격'
           WHEN ranked.position <= ranked.issue_amount THEN '예비'
           ELSE '불합격'
       END,
       CASE
           WHEN ranked.position <= ranked.init_sector_capacity THEN -2
           WHEN ranked.position <= ranked.issue_amount
               THEN ranked.position - ranked.init_sector_capacity
           ELSE -1
       END
FROM (
    SELECT registration.id AS registration_id,
           ROW_NUMBER() OVER (
               PARTITION BY registration.sector_id
               ORDER BY registration.saved_at ASC, registration.id ASC
           ) AS position,
           sector.init_sector_capacity,
           sector.issue_amount
    FROM registration_tb registration
    INNER JOIN sector ON sector.sector_id = registration.sector_id
    WHERE registration.is_saved = b'1'
      AND registration.is_deleted = b'0'
) ranked;

SET @partial_result_count = (
    SELECT COUNT(*)
    FROM registration_tb
    WHERE is_saved = b'1'
      AND is_deleted = b'0'
      AND NOT (
          (position IS NULL AND result_status IS NULL AND sequence IS NULL)
          OR
          (position IS NOT NULL AND result_status IS NOT NULL AND sequence IS NOT NULL)
      )
);
SET @validation_statement = IF(
    @partial_result_count = 0,
    'SELECT 1',
    'SELECT 1 FROM __flyway_blocked_registration_partial_result'
);
PREPARE validation_statement FROM @validation_statement;
EXECUTE validation_statement;
DEALLOCATE PREPARE validation_statement;

SET @mixed_result_sector_count = (
    SELECT COUNT(*)
    FROM (
        SELECT sector_id
        FROM registration_tb
        WHERE is_saved = b'1'
          AND is_deleted = b'0'
        GROUP BY sector_id
        HAVING SUM(position IS NULL) > 0
           AND SUM(position IS NOT NULL) > 0
    ) mixed_sector
);
SET @validation_statement = IF(
    @mixed_result_sector_count = 0,
    'SELECT 1',
    'SELECT 1 FROM __flyway_blocked_registration_mixed_sector_result'
);
PREPARE validation_statement FROM @validation_statement;
EXECUTE validation_statement;
DEALLOCATE PREPARE validation_statement;

SET @invalid_existing_position_count = (
    SELECT COUNT(*)
    FROM (
        SELECT sector_id
        FROM registration_tb
        WHERE is_saved = b'1'
          AND is_deleted = b'0'
          AND position IS NOT NULL
        GROUP BY sector_id
        HAVING MIN(position) <> 1
            OR MAX(position) <> COUNT(*)
            OR COUNT(DISTINCT position) <> COUNT(*)
    ) invalid_sector
);
SET @validation_statement = IF(
    @invalid_existing_position_count = 0,
    'SELECT 1',
    'SELECT 1 FROM __flyway_blocked_registration_invalid_existing_position'
);
PREPARE validation_statement FROM @validation_statement;
EXECUTE validation_statement;
DEALLOCATE PREPARE validation_statement;

SET @invalid_existing_result_count = (
    SELECT COUNT(*)
    FROM registration_tb registration
    INNER JOIN sector ON sector.sector_id = registration.sector_id
    WHERE registration.is_saved = b'1'
      AND registration.is_deleted = b'0'
      AND registration.position IS NOT NULL
      AND NOT (
          (registration.position <= sector.init_sector_capacity
              AND registration.result_status = '합격'
              AND registration.sequence = -2)
          OR
          (registration.position > sector.init_sector_capacity
              AND registration.position <= sector.issue_amount
              AND registration.result_status = '예비'
              AND registration.sequence = registration.position - sector.init_sector_capacity)
          OR
          (registration.position > sector.issue_amount
              AND registration.result_status = '불합격'
              AND registration.sequence = -1)
      )
);
SET @validation_statement = IF(
    @invalid_existing_result_count = 0,
    'SELECT 1',
    'SELECT 1 FROM __flyway_blocked_registration_invalid_existing_result'
);
PREPARE validation_statement FROM @validation_statement;
EXECUTE validation_statement;
DEALLOCATE PREPARE validation_statement;

UPDATE registration_tb registration
INNER JOIN registration_result_backfill backfill
    ON backfill.registration_id = registration.id
SET registration.position = backfill.position,
    registration.result_status = backfill.result_status,
    registration.sequence = backfill.sequence
WHERE registration.position IS NULL
  AND registration.result_status IS NULL
  AND registration.sequence IS NULL;

SET @missing_result_count = (
    SELECT COUNT(*)
    FROM registration_tb
    WHERE is_saved = b'1'
      AND is_deleted = b'0'
      AND (position IS NULL OR result_status IS NULL OR sequence IS NULL)
);
SET @validation_statement = IF(
    @missing_result_count = 0,
    'SELECT 1',
    'SELECT 1 FROM __flyway_failed_registration_result_missing'
);
PREPARE validation_statement FROM @validation_statement;
EXECUTE validation_statement;
DEALLOCATE PREPARE validation_statement;

SET @invalid_position_count = (
    SELECT COUNT(*)
    FROM (
        SELECT sector_id
        FROM registration_tb
        WHERE is_saved = b'1'
          AND is_deleted = b'0'
        GROUP BY sector_id
        HAVING MIN(position) <> 1
            OR MAX(position) <> COUNT(*)
            OR COUNT(DISTINCT position) <> COUNT(*)
    ) invalid_sector
);
SET @validation_statement = IF(
    @invalid_position_count = 0,
    'SELECT 1',
    'SELECT 1 FROM __flyway_failed_registration_position_not_contiguous'
);
PREPARE validation_statement FROM @validation_statement;
EXECUTE validation_statement;
DEALLOCATE PREPARE validation_statement;

SET @invalid_result_count = (
    SELECT COUNT(*)
    FROM registration_tb registration
    INNER JOIN sector ON sector.sector_id = registration.sector_id
    WHERE registration.is_saved = b'1'
      AND registration.is_deleted = b'0'
      AND NOT (
          (registration.position <= sector.init_sector_capacity
              AND registration.result_status = '합격'
              AND registration.sequence = -2)
          OR
          (registration.position > sector.init_sector_capacity
              AND registration.position <= sector.issue_amount
              AND registration.result_status = '예비'
              AND registration.sequence = registration.position - sector.init_sector_capacity)
          OR
          (registration.position > sector.issue_amount
              AND registration.result_status = '불합격'
              AND registration.sequence = -1)
      )
);
SET @validation_statement = IF(
    @invalid_result_count = 0,
    'SELECT 1',
    'SELECT 1 FROM __flyway_failed_registration_result_mismatch'
);
PREPARE validation_statement FROM @validation_statement;
EXECUTE validation_statement;
DEALLOCATE PREPARE validation_statement;

DROP TEMPORARY TABLE registration_result_backfill;
