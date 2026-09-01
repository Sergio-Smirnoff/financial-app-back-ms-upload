package com.financialapp.upload.domain.model.importrun;

import java.util.Objects;

public record ImportRunId(Long value) {

    public ImportRunId {
        Objects.requireNonNull(value, "importRunId value must not be null");
        if (value <= 0) {
            throw new IllegalArgumentException("importRunId value must be positive");
        }
    }
}
