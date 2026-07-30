package com.financialapp.upload.web.mapper;

import com.financialapp.upload.domain.model.importrun.ImportRun;
import com.financialapp.upload.domain.model.importrun.ImportRunStatus;
import com.financialapp.upload.web.dto.response.ImportRunResponse;
import org.springframework.stereotype.Component;

@Component
public class ImportRunWebMapper {

    public ImportRunResponse toResponse(ImportRun domain) {
        if (domain == null) return null;

        boolean canUndo = domain.status() == ImportRunStatus.COMPLETED || domain.status() == ImportRunStatus.PARTIAL;

        return ImportRunResponse.builder()
                .id(domain.id() != null ? domain.id().value() : null)
                .bankNumber(domain.bankNumber().value())
                .accountCbu(domain.accountCbu().value())
                .periodFrom(domain.period().from())
                .periodTo(domain.period().to())
                .status(domain.status().name())
                .importedCount(domain.importedCount())
                .skippedCount(domain.skippedCount())
                .reconciliation(domain.reconciliation())
                .createdAt(domain.createdAt())
                .canUndo(canUndo)
                .build();
    }
}
