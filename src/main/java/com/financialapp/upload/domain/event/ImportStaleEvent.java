package com.financialapp.upload.domain.event;

import com.financialapp.commons.core.domain.model.Cbu;
import com.financialapp.upload.domain.common.model.BankNumber;
import com.financialapp.upload.domain.common.model.UserId;
import com.financialapp.upload.domain.model.importrun.ImportRunId;

import java.util.Objects;

public record ImportStaleEvent(
        ImportRunId importRunId,
        UserId userId,
        Cbu accountCbu,
        BankNumber bankNumber,
        int daysSinceImport
) implements DomainEvent {
    public ImportStaleEvent {
        Objects.requireNonNull(importRunId, "importRunId must not be null");
        Objects.requireNonNull(userId, "userId must not be null");
        Objects.requireNonNull(accountCbu, "accountCbu must not be null");
        Objects.requireNonNull(bankNumber, "bankNumber must not be null");
        if (daysSinceImport < 0) {
            throw new IllegalArgumentException("daysSinceImport must not be negative");
        }
    }
}
