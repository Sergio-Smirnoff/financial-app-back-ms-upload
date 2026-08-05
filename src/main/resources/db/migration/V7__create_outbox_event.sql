CREATE TABLE upload.outbox_event (
    id            BIGSERIAL PRIMARY KEY,
    event_id      VARCHAR(64)  NOT NULL UNIQUE,
    topic         VARCHAR(249) NOT NULL,
    aggregate_key VARCHAR(64)  NOT NULL,
    ce_type       VARCHAR(120) NOT NULL,
    ce_source     VARCHAR(255) NOT NULL,
    data_schema   VARCHAR(512) NOT NULL,
    data_json     TEXT         NOT NULL,
    sent          BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at    TIMESTAMP    NOT NULL DEFAULT NOW(),
    sent_at       TIMESTAMP
);

CREATE INDEX idx_upload_outbox_unsent ON upload.outbox_event (id) WHERE sent = FALSE;
