package com.financialapp.upload.domain.common.model;

import java.time.LocalDate;
import java.util.Objects;

public record DateRange(LocalDate from, LocalDate to) {

    public DateRange {
        Objects.requireNonNull(from, "from date must not be null");
        Objects.requireNonNull(to, "to date must not be null");
        if (to.isBefore(from)) {
            throw new IllegalArgumentException("to date must not be before from date");
        }
    }
}
