ALTER TABLE upload.statement_imports
  DROP CONSTRAINT uq_statement_import,
  ADD COLUMN original_name VARCHAR(255),
  ADD COLUMN file_hash     VARCHAR(64),
  ADD COLUMN bank_id       BIGINT,
  ADD COLUMN account_id    BIGINT,
  ADD COLUMN card_id       BIGINT,
  ALTER COLUMN account_number DROP NOT NULL,
  ALTER COLUMN period_key     DROP NOT NULL;

CREATE UNIQUE INDEX uq_stmt_import_file_hash
  ON upload.statement_imports (user_id, file_hash)
  WHERE file_hash IS NOT NULL;
