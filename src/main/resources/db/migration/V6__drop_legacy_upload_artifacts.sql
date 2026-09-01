ALTER TABLE upload.statement_imports
    DROP COLUMN IF EXISTS account_number,
    DROP COLUMN IF EXISTS period_key;

DROP TABLE IF EXISTS upload.files;
