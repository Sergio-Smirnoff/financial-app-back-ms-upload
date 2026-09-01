CREATE TABLE upload.import_run_transactions (
    import_run_id  BIGINT NOT NULL REFERENCES upload.import_runs (id),
    transaction_id BIGINT NOT NULL,
    PRIMARY KEY (import_run_id, transaction_id)
);
