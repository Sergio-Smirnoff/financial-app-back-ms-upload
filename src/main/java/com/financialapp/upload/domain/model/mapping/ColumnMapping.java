package com.financialapp.upload.domain.model.mapping;

import java.util.Objects;

public record ColumnMapping(
        int dateCol,
        int descriptionCol,
        AmountMapping amountMapping,
        Integer balanceCol,
        String dateFormat
) {
    public ColumnMapping {
        Objects.requireNonNull(amountMapping, "amountMapping must not be null");
    }
}
