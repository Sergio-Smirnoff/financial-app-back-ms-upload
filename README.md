# ms-upload

Bank-statement upload service. Accepts PDF or CSV files, stores them in MinIO, parses them into candidate transactions, returns a preview, and on confirmation bulk-imports rows into ms-finances.

**Port:** 8085  
**DB schema:** `upload`  
**Storage:** MinIO (`statements` bucket, `receipts` bucket)  
**Framework:** Spring MVC + Spring Boot 3.4.2, Java 21  
**External calls:** ms-finances (Feign), ms-banks (Feign)

---

## Upload → Preview → Confirm Flow

1. Frontend sends `POST /statement/preview` (multipart: file + FileType).
2. Service stores the raw file in MinIO at `temp/{uuid}/{filename}`, saves an `UploadSession`, parses the stream, returns `StatementPreviewResponse`.
3. User reviews rows in `ImportPreviewDialog`, selects account, adjusts categories, clicks Confirm.
4. Frontend sends `POST /statement/confirm` with `tempKey`, `accountId`, `fileType`, and per-row `mappings[]`.
5. Service validates session ownership, calls `FinancesClient.createTransaction` for each row (errors logged, not thrown), records a `StatementImport` audit row with status `COMPLETED`.

---

## Endpoints

All paths are under `/api/v1/upload`. `X-User-Id` is injected by the gateway — never sent by the frontend.

| Method | Path | Request | Response data |
|--------|------|---------|---------------|
| `POST` | `/statement/preview` | `multipart/form-data`: `file`, `fileType` (FileType) | `StatementPreviewResponse` — `tempKey`, `accountNumber`, `transactions[]`, `totalAmount`, `count` |
| `POST` | `/statement/confirm` | JSON `StatementConfirmRequest` — `tempKey`, `accountId`, `fileType`, `mappings[]` | `StatementConfirmResponse` — `importId`, `status`, `importedCount` |
| `POST` | `/csv/preview` | `multipart/form-data`: `file` (CSV) | `CsvPreviewResponse` — `tempKey`, `headers[]`, first 5 `rows[][]` |
| `POST` | `/csv/confirm` | JSON `CsvConfirmRequest` — `tempKey`, `accountId`, `dateCol`, `descCol`, `debitCol`, `creditCol`, `dateFormat`, `mappings[]` | `CsvImportResponse` — `importId`, `status`, `importedCount` |
| `GET`  | `/history` | — | `StatementImport[]` for the authenticated user, newest first |

All responses use the platform envelope: `ApiResponse<T>` (`success`, `message`, `data`, `errors`, `timestamp`).

---

## Supported File Types

| `FileType` | Parser | Source |
|------------|--------|--------|
| `BANK_PDF` | `ICBCBankMovementsPdfParser` | ICBC bank movements PDF (ARS debit/credit columns) |
| `VISA_PDF` | `ICBCVisaPdfParser` | ICBC VISA credit card statement PDF (ARS + USD) |
| `CSV` | `GenericCsvParser` | Generic CSV, configurable column mapping, auto-detected date format |

---

## Folder Tree

```
back/ms-upload/src/main/java/com/financialapp/upload/
├── UploadApplication.java
├── client/
│   ├── BanksClient.java              Feign → ms-banks card installments import + dup-check
│   └── FinancesClient.java           Feign → ms-finances transaction create + dup-check
├── config/
│   ├── BucketInitializer.java        creates MinIO buckets on startup
│   ├── FeignConfig.java
│   ├── InternalAuthFilter.java
│   ├── MinioConfig.java              MinioClient bean + bucket name properties
│   └── SwaggerConfig.java
├── controller/
│   └── StatementController.java      all upload endpoints
├── exception/
│   ├── BusinessException.java
│   ├── GlobalExceptionHandler.java
│   ├── InvalidFileException.java
│   ├── ParseException.java
│   └── ResourceNotFoundException.java
├── model/
│   ├── common/
│   │   └── Money.java
│   ├── dto/
│   │   ├── ParsedTransaction.java              date, description, amount, currency, type
│   │   ├── request/
│   │   │   ├── CardExpenseCreateRequest.java
│   │   │   ├── CardExpenseImportRequest.java
│   │   │   ├── ConfirmRequest.java
│   │   │   ├── CsvConfirmRequest.java          tempKey, accountId, col mappings, dateFormat, mappings[]
│   │   │   ├── ResolveRequest.java
│   │   │   ├── StatementConfirmRequest.java    tempKey, accountId, fileType, mappings[]
│   │   │   ├── TransactionMappingRequest.java  date, description, amount, currency, type, categoryId
│   │   │   └── TransactionRequest.java         forwarded to ms-finances
│   │   └── response/
│   │       ├── ApiResponse.java
│   │       ├── BatchImportResponse.java
│   │       ├── ConfirmResponse.java
│   │       ├── CsvImportResponse.java          importId, status, importedCount
│   │       ├── CsvPreviewResponse.java         tempKey, headers[], rows[][]
│   │       ├── ImportHistoryRecord.java
│   │       ├── PreviewResponse.java
│   │       ├── ResolveResponse.java
│   │       ├── StatementConfirmResponse.java   importId, status, importedCount
│   │       └── StatementPreviewResponse.java   tempKey, accountNumber, transactions[], totalAmount, count
│   ├── entity/
│   │   ├── StatementImport.java     audit record (upload.statement_imports)
│   │   └── UploadSession.java       temp session keyed by MinIO path (upload.upload_sessions)
│   └── enums/
│       ├── FileType.java            VISA_PDF | BANK_PDF | CSV
│       ├── ImportStatus.java        PENDING | COMPLETED | FAILED | PARTIAL
│       └── TransactionType.java     INCOME | EXPENSE
├── parser/
│   ├── StatementParser.java                 interface: parse(InputStream, Map)
│   ├── ICBCBankMovementsPdfParser.java      PDFBox; regex on date + amount columns; infers year from PERIODO header
│   ├── ICBCVisaPdfParser.java               PDFBox; section-scoped; ARS + USD
│   └── GenericCsvParser.java               OpenCSV; auto-detects date format from 7 patterns; configurable columns
├── repository/
│   ├── StatementImportRepository.java
│   └── UploadSessionRepository.java
└── service/
    ├── MinioStorageService.java    store / retrieve / move / delete against MinIO
    ├── ParsingService.java         dispatches to correct StatementParser by FileType
    └── StatementService.java       orchestrates: store → parse → confirm → forward → record
```

---

## Database Schema (`upload`)

### Flyway migrations

| Version | Description |
|---------|-------------|
| V1 | `statement_imports` table + `files` table |
| V2 | Drop unique constraint; add `original_name`, `file_hash`, `bank_id`, `account_id`, `card_id`; unique index on `(user_id, file_hash)` |
| V3 | `upload_sessions` table (keyed by `temp_key`) |

### Tables

- **`statement_imports`** — audit record per import run: `user_id`, `file_type`, `original_name`, `file_hash`, `account_id`, `minio_path`, `imported_count`, `import_status`, `created_at`
- **`upload_sessions`** — temporary session: `temp_key` (PK), `user_id`, `created_at`
- **`files`** — general file metadata (future use)

---

## MinIO Buckets

| Env var | Default | Purpose |
|---------|---------|---------|
| `MINIO_BUCKET_STATEMENTS` | `statements` | All statement PDFs and CSV exports |
| `MINIO_BUCKET_RECEIPTS` | `receipts` | Receipt files (future use) |

Temporary uploads land at `temp/{uuid}/{originalFilename}` inside `statements`.

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
