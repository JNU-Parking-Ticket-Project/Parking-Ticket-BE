ALTER TABLE event
    ADD COLUMN admission_mode VARCHAR(32) NOT NULL DEFAULT 'REDIS' AFTER event_status,
    ADD COLUMN admission_epoch BIGINT NOT NULL DEFAULT 0 AFTER admission_mode,
    ADD COLUMN version BIGINT NOT NULL DEFAULT 0 AFTER admission_epoch;

CREATE TABLE registration_admission_journal
(
    id                   BIGINT AUTO_INCREMENT PRIMARY KEY,
    event_id             BIGINT        NOT NULL,
    sector_id            BIGINT        NOT NULL,
    user_id              BIGINT        NOT NULL,
    email                VARCHAR(255)  NOT NULL,
    admission_epoch      BIGINT        NOT NULL,
    state                VARCHAR(32)   NOT NULL,
    decision_source      VARCHAR(32)   NULL,
    decision_reason      VARCHAR(32)   NULL,
    position             INT           NULL,
    result_status        VARCHAR(255)  NULL,
    sequence             INT           NULL,
    remaining_amount     INT           NULL,
    registration_payload VARCHAR(10000) NOT NULL,
    payload_version      INT           NOT NULL DEFAULT 1,
    received_at          BIGINT        NOT NULL,
    decided_at           BIGINT        NULL,
    materialized_at      BIGINT        NULL,
    registration_id      BIGINT        NULL,
    version              BIGINT        NOT NULL DEFAULT 0,
    CONSTRAINT uk_admission_journal_event_email UNIQUE (event_id, email),
    CONSTRAINT uk_admission_journal_sector_position UNIQUE (sector_id, position),
    CONSTRAINT uk_admission_journal_registration UNIQUE (registration_id),
    CONSTRAINT chk_admission_journal_state
        CHECK (state IN ('RECEIVED', 'DECIDED', 'MATERIALIZED', 'REJECTED')),
    CONSTRAINT chk_admission_journal_position
        CHECK (position IS NULL OR position > 0),
    CONSTRAINT chk_admission_journal_remaining
        CHECK (remaining_amount IS NULL OR remaining_amount >= 0),
    CONSTRAINT chk_admission_journal_payload_version
        CHECK (payload_version = 1),
    CONSTRAINT chk_admission_journal_decision
        CHECK (
            (state = 'RECEIVED' AND decision_source IS NULL AND position IS NULL)
            OR (state IN ('DECIDED', 'MATERIALIZED') AND decision_source IS NOT NULL AND position IS NOT NULL)
            OR (state = 'REJECTED' AND decision_reason IS NOT NULL AND position IS NULL)
        ),
    INDEX idx_admission_journal_event_state (event_id, state, id),
    INDEX idx_admission_journal_state (state, id)
);
