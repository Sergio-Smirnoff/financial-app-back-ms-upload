package com.financialapp.upload.infrastructure.messaging.mapper;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.financialapp.commons.messaging.domain.model.EventType;
import com.financialapp.commons.messaging.domain.model.OutboxRecord;
import com.financialapp.upload.domain.event.ImportStaleEvent;
import com.financialapp.upload.infrastructure.messaging.payload.ImportStaleData;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ImportStaleMapper extends JsonTypedDomainEventMapper<ImportStaleEvent> {

    public static final String TOPIC = "upload.import.stale";
    static final String SCHEMA = "https://schemas.financial-app/upload/import-stale/v1";
    static final String SOURCE = "ms-upload";

    public ImportStaleMapper(ObjectMapper objectMapper) {
        super(ImportStaleEvent.class, objectMapper);
    }

    @Override
    protected List<OutboxRecord> mapTyped(ImportStaleEvent event) {
        ImportStaleData data = new ImportStaleData(
                event.userId().value(),
                event.accountCbu().cbuNumber(),
                event.bankNumber().value(),
                event.daysSinceImport());

        return List.of(OutboxRecord.create(
                TOPIC,
                String.valueOf(event.importRunId().value()),
                new EventType(TOPIC),
                SOURCE,
                SCHEMA,
                serialize(data)));
    }
}
