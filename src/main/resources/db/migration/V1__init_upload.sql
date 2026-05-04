CREATE TABLE upload.statement_imports (
    id             BIGSERIAL    PRIMARY KEY,
    user_id        BIGINT       NOT NULL,
    file_type      VARCHAR(50)  NOT NULL,
    account_number VARCHAR(100) NOT NULL,
    period_key     VARCHAR(50)  NOT NULL,
    minio_path     VARCHAR(500) NOT NULL,
    imported_count INT          NOT NULL DEFAULT 0,
    import_status  VARCHAR(20)  NOT NULL,
    created_at     TIMESTAMP    NOT NULL,
    CONSTRAINT uq_statement_import UNIQUE (user_id, file_type, account_number, period_key)
);
CREATE INDEX idx_stmt_import_user ON upload.statement_imports (user_id);
CREATE TABLE upload.files (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    bank_account_id BIGINT,
    original_name VARCHAR(255) NOT NULL,
    storage_path VARCHAR(500) NOT NULL,
    content_type VARCHAR(100),
    size_bytes BIGINT,
    status VARCHAR(50) NOT NULL, -- UPLOADED, PARSED, PROCESSED, ERROR
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
