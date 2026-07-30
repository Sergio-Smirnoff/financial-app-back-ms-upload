package com.financialapp.upload.domain.common.model;

import java.util.Objects;

public record UserId(Long value) {

    public UserId {
        Objects.requireNonNull(value, "userId must not be null");
        if (value <= 0) {
            throw new IllegalArgumentException("userId must be positive");
        }
    }
}
