CREATE TABLE upload.import_runs (
    id                  BIGSERIAL PRIMARY KEY,
    user_id             BIGINT      NOT NULL,
    bank_number         VARCHAR(3)  NOT NULL,
    account_cbu         VARCHAR(22) NOT NULL,
    file_hash           CHAR(64)    NOT NULL,
    period_from         DATE        NOT NULL,
    period_to           DATE        NOT NULL,
    status              VARCHAR(20) NOT NULL,
    imported_count      INT         NOT NULL DEFAULT 0,
    skipped_count       INT         NOT NULL DEFAULT 0,
    reconciliation      JSONB,
    last_stale_alert_at TIMESTAMP,
    created_at          TIMESTAMP   NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX uq_import_runs_user_active_file_hash
    ON upload.import_runs (user_id, file_hash)
    WHERE status <> 'UNDONE';
