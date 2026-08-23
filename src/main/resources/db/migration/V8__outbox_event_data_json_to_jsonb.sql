-- V7 created upload.outbox_event.data_json as TEXT, diverging from the commons
-- OutboxRecordEntity contract (@JdbcTypeCode(SqlTypes.JSON), columnDefinition = "jsonb")
-- that banks, finances, investments and users all already satisfy. Align upload with it
-- so Hibernate schema validation passes without a per-service @AttributeOverride.
ALTER TABLE upload.outbox_event
    ALTER COLUMN data_json TYPE JSONB USING data_json::jsonb;
