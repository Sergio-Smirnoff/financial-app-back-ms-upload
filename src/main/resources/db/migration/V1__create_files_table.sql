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
