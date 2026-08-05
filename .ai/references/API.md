# ms-upload — API

Endpoints and error codes. Envelope shape: parent `.ai/references/APP_STRUCTURE.md` — not repeated here.

## Endpoints

| Method | Path | Purpose | Error codes |
|---|---|---|---|
| POST | `/api/v1/upload/statement/preview` | Upload PDF/CSV statement file for parsing & preview | `invalid_file_format`, `file_size_exceeded`, `parsing_error` |
| POST | `/api/v1/upload/statement/confirm` | Confirm parsed statement import & record transactions | `duplicate_import`, `session_expired`, `finances_service_unavailable` |
| POST | `/api/v1/upload/csv/preview` | Preview generic CSV header and sample rows | `invalid_file_format`, `file_size_exceeded` |
| POST | `/api/v1/upload/csv/confirm` | Confirm CSV import with custom column mappings | `duplicate_import`, `invalid_column_mapping`, `finances_service_unavailable` |
| GET | `/api/v1/upload/history` | List import history for the authenticated user | — |
| POST | `/api/v1/upload/runs/{id}/undo` | Reverse an import run (deletes created transactions) | `resource_not_found`, `import_already_undone` |
| GET | `/api/v1/upload/runs/{id}` | Get single import run detail and reconciliation | `resource_not_found` |
| GET | `/api/v1/upload/runs/by-transaction/{transactionId}` | Find origin import run for a transaction ID | `resource_not_found` |

## Upload Limits & Content Types

- Max file size: **20 MB** per upload (`spring.servlet.multipart.max-file-size`).
- Supported MIME types: `application/pdf`, `text/csv`, `application/vnd.ms-excel`.

## DomainError catalog

| Slug | HTTP status | When it is thrown |
|---|---|---|
| `resource_not_found` | 404 | Import run or transaction mapping lookup returned no match |
| `duplicate_import` | 409 | Identical active file hash already imported by user |
| `import_already_undone` | 409 | `undo` attempted on an import run already in `UNDONE` status |
| `session_expired` | 404 | `tempKey` lookup in `upload_sessions` expired or not found |
| `invalid_file_format` | 400 | File extension or MIME type not supported for selected parser |
| `file_size_exceeded` | 400 | Uploaded file exceeds 20 MB ceiling |
| `invalid_column_mapping` | 400 | Required CSV column index missing or out of bounds |
| `parsing_error` | 422 | Statement parser failed to extract structured rows |
| `finances_service_unavailable` | 500 | Feign call to ms-finances failed during transaction posting |
| `internal_error` | 500 | Unmapped failure |
