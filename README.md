# ms-upload

Bank-statement upload service. Accepts PDF or CSV files, stores them in MinIO, parses them into candidate transactions, returns a preview, and on confirmation bulk-imports rows into ms-finances via `ImportRun` aggregate with SHA-256 file hash dedup, undo capability, and statement reconciliation.

**Port:** 8085  
**DB schema:** `upload`  
**Storage:** MinIO (`statements` bucket, `receipts` bucket)  
**Framework:** Spring MVC + Spring Boot 3.4.2, Java 21 (Hexagonal Architecture)  
**External calls:** ms-finances (Feign), ms-banks (Feign)

---

## Upload → Preview → Confirm Flow

1. Frontend sends `POST /statement/preview` (multipart: file + FileType).
2. Service stores the raw file in MinIO at `temp/{uuid}/{filename}`, saves an `UploadSession`, parses the stream, returns `StatementPreviewResponse`.
3. User reviews rows in `ImportPreviewDialog`, selects account, adjusts categories, clicks Confirm.
4. Frontend sends `POST /statement/confirm` or `POST /csv/confirm` with `tempKey`, `accountId`, `fileType`, mapping options, and per-row `mappings[]`.
5. Service executes `ConfirmImportUseCase`:
   - Computes `FileHash` (SHA-256) of uploaded bytes and checks for active duplicates (`status <> 'UNDONE'`).
   - Calls `FinancesClient.createTransaction` for each row (errors logged and counted as skipped).
   - Computes `ReconciliationResult` (statement balance vs calculated sum of created rows).
   - Moves file from `temp/` to `imports/{userId}/{importRunId}/...`.
   - Records `ImportRun` aggregate and `import_run_transactions` join records.

---

## Endpoints

All paths are under `/api/v1/upload`. `X-User-Id` is injected by the gateway — never sent by the frontend.

| Method | Path | Request | Response data |
|--------|------|---------|---------------|
| `POST` | `/statement/preview` | `multipart/form-data`: `file`, `fileType` (FileType) | `StatementPreviewResponse` — `tempKey`, `accountNumber`, `transactions[]`, `totalAmount`, `count` |
| `POST` | `/statement/confirm` | JSON `StatementConfirmRequest` — `tempKey`, `accountId`, `fileType`, `mappings[]` | `StatementConfirmResponse` — `importId`, `status`, `importedCount` |
| `POST` | `/csv/preview` | `multipart/form-data`: `file` (CSV) | `CsvPreviewResponse` — `tempKey`, `headers[]`, first 5 `rows[][]` |
| `POST` | `/csv/confirm` | JSON `CsvConfirmRequest` — `tempKey`, `accountId`, `dateCol`, `descCol`, `debitCol`, `creditCol`, `montoCol`, `balanceCol`, `dateFormat`, `mappings[]` | `CsvImportResponse` — `importId`, `status`, `importedCount` |
| `GET`  | `/history` | — | `ImportRunResponse[]` for the authenticated user, newest first |
| `POST` | `/runs/{id}/undo` | — | `UndoResultResponse` — `deletedCount`, `skippedCount`, `skippedTransactionIds[]` |
| `GET`  | `/runs/{id}` | — | `ImportRunResponse` for single run incl. reconciliation |
| `GET`  | `/runs/by-transaction/{transactionId}` | — | `ImportRunResponse` for origin run of transaction (or 404) |

All responses use the shared envelope `{ status, title, code, message, data }` from `commons-core`.

---

## Supported File Types

| `FileType` | Parser | Source |
|------------|--------|--------|
| `BANK_PDF` | `ICBCBankMovementsPdfParser` | ICBC bank movements PDF (ARS debit/credit columns) |
| `VISA_PDF` | `ICBCVisaPdfParser` | ICBC VISA credit card statement PDF (ARS + USD) |
| `CSV` | `GenericCsvParser` | Generic CSV, configurable column mapping (`SeparateDebitCredit` / `SingleSignedColumn`), balance column extraction, auto-detected date format |

---

## Retention & Automation

- `ImportRetentionScheduler`: Daily scheduled sweep (`03:00 AM`) deleting MinIO objects under `imports/` older than 30 days.

---

## Database Schema (`upload`)

### Flyway migrations

| Version | Description |
|---------|-------------|
| V1 | `statement_imports` table + `files` table |
| V2 | Drop unique constraint; add `original_name`, `file_hash`, `bank_id`, `account_id`, `card_id`; unique index on `(user_id, file_hash)` |
| V3 | `upload_sessions` table (keyed by `temp_key`) |
| V4 | `import_runs` table + partial unique index on active `(user_id, file_hash)` |
| V5 | `import_run_transactions` normalized join table |
| V6 | Drop legacy `files` table and orphan `account_number`, `period_key` columns from `statement_imports` |

---

## Key Env Vars

| Variable | Default | Purpose |
|----------|---------|---------|
| `MINIO_ENDPOINT` | `http://localhost:9000` | MinIO server URL |
| `MINIO_ACCESS_KEY` | `minioadmin` | MinIO credentials |
| `MINIO_SECRET_KEY` | `changeme` | MinIO credentials |
| `MINIO_BUCKET_STATEMENTS` | `statements` | Statements bucket name |
| `MINIO_BUCKET_RECEIPTS` | `receipts` | Receipts bucket name |
| `FINANCES_SERVICE_URL` | `http://localhost:8082` | ms-finances Feign target |
| `BANKS_SERVICE_URL` | `http://localhost:8083` | ms-banks Feign target |
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://localhost:5432/financialapp` | Postgres connection |

Max upload size: **20 MB** per file.

---

## Run

```bash
# Via parent workspace dev script (recommended)
./scripts/dev.sh local service-upload

# Direct Maven
cd back/ms-upload
mvn spring-boot:run

# Swagger UI
http://localhost:8085/swagger-ui.html
```

> Full design: `docs/specs/services/ms-upload.md` (parent workspace).
