# ms-upload — messaging and jobs

CloudEvents 1.0, Kafka binary mode, via `commons-messaging`. Topic name = `ce_type`. Outbox conventions: parent `.ai/references/ARCHITECTURE.md` — not repeated here.

## Published

| ce_type / topic | when emitted | payload fields |
|---|---|---|
| `upload.import.completed` | Import run confirmation completes successfully | importRunId, userId, fileType, importedCount, totalAmount |

## Consumed

ms-upload is a REST-only service and **consumes no Kafka events** (carries an unused `spring-kafka` dependency).

## Scheduled jobs

| Job | Trigger / Cron | What it does |
|---|---|---|
| `ImportRetentionScheduler.cleanTempStorage` | `0 0 3 * * *` (daily 03:00 AM) | Sweeps MinIO statement objects under `temp/` older than 30 days |

## Outbound calls

| Target service / Store | Client | Purpose |
|---|---|---|
| MinIO Object Storage | `MinioClient` (`statements`, `receipts` buckets) | Stores temporary preview files and archived import statements |
| ms-finances | `FinancesClient` (Feign) | Forwards parsed transaction rows (`POST /transactions`) and handles undo (`DELETE /transactions/{id}`) |
| ms-banks | `BanksClient` (Feign, uninjected) | Card installment import path interface |
