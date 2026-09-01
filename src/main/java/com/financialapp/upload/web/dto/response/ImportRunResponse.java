package com.financialapp.upload.web.dto.response;
import com.financialapp.commons.core.domain.model.Cbu;

import com.financialapp.upload.domain.model.importrun.ReconciliationResult;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImportRunResponse {
    private Long id;
    private String bankNumber;
    private String accountCbu;
    private LocalDate periodFrom;
    private LocalDate periodTo;
    private String status;
    private int importedCount;
    private int skippedCount;
    private ReconciliationResult reconciliation;
    private LocalDateTime createdAt;
    private boolean canUndo;
}
