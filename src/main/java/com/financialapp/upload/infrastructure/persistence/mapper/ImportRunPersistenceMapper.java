package com.financialapp.upload.infrastructure.persistence.mapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.financialapp.upload.domain.common.model.BankNumber;
import com.financialapp.commons.core.domain.model.Cbu;
import com.financialapp.upload.domain.common.model.DateRange;
import com.financialapp.upload.domain.common.model.UserId;
import com.financialapp.upload.domain.model.importrun.*;
import com.financialapp.upload.infrastructure.persistence.entity.ImportRunJpaEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;

@Component
@RequiredArgsConstructor
public class ImportRunPersistenceMapper {

    private final ObjectMapper objectMapper;

    public ImportRunJpaEntity toEntity(ImportRun domain) {
        if (domain == null) return null;

        String reconciliationJson = null;
        if (domain.reconciliation() != null) {
            try {
                reconciliationJson = objectMapper.writeValueAsString(domain.reconciliation());
            } catch (JsonProcessingException e) {
                throw new RuntimeException("Failed to serialize ReconciliationResult to JSON", e);
            }
        }

        return ImportRunJpaEntity.builder()
                .id(domain.id() != null ? domain.id().value() : null)
                .userId(domain.userId().value())
                .bankNumber(domain.bankNumber().value())
                .accountCbu(domain.accountCbu().value())
                .fileHash(domain.fileHash().value())
                .periodFrom(domain.period().from())
                .periodTo(domain.period().to())
                .status(domain.status().name())
                .importedCount(domain.importedCount())
                .skippedCount(domain.skippedCount())
                .reconciliationJson(reconciliationJson)
                .lastStaleAlertAt(domain.lastStaleAlertAt())
                .createdAt(domain.createdAt())
                .transactionIds(new ArrayList<>(domain.createdTransactionIds()))
                .build();
    }

    public ImportRun toDomain(ImportRunJpaEntity entity) {
        if (entity == null) return null;

        ReconciliationResult reconciliation = null;
        if (entity.getReconciliationJson() != null && !entity.getReconciliationJson().isBlank()) {
            try {
                reconciliation = objectMapper.readValue(entity.getReconciliationJson(), ReconciliationResult.class);
            } catch (JsonProcessingException e) {
                throw new RuntimeException("Failed to deserialize ReconciliationResult from JSON", e);
            }
        }

        return ImportRun.reconstitute(
                entity.getId() != null ? new ImportRunId(entity.getId()) : null,
                new UserId(entity.getUserId()),
                new BankNumber(entity.getBankNumber()),
                new Cbu(entity.getAccountCbu()),
                new FileHash(entity.getFileHash()),
                new DateRange(entity.getPeriodFrom(), entity.getPeriodTo()),
                ImportRunStatus.valueOf(entity.getStatus()),
                entity.getTransactionIds(),
                entity.getImportedCount(),
                entity.getSkippedCount(),
                reconciliation,
                entity.getLastStaleAlertAt(),
                entity.getCreatedAt()
        );
    }
}
