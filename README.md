# financial-app-upload

Upload microservice — file storage on MinIO, automatic PDF statement parsing per bank.

## Port: 8085

## Database Schema: `upload` (metadata only — files stored in MinIO)

## Endpoints
```
POST   /api/v1/upload/files
GET    /api/v1/upload/files
GET    /api/v1/upload/files/{id}
DELETE /api/v1/upload/files/{id}
POST   /api/v1/upload/files/{id}/process
GET    /api/v1/upload/templates
```

## Kafka — Publishes
- `card.statement.uploaded`

## PDF Parsing
Each bank has a parser class implementing `StatementParser`. The bank is identified from the uploaded PDF and the corresponding parser is instantiated automatically.

## Environment Variables
See `.env.example`.

## Local Development

```bash
cd ../financial-app-parent && mvn install -N
cd ../financial-app-upload
cp .env.example .env
mvn spring-boot:run
```

## Swagger
`http://localhost:8085/swagger-ui.html`
