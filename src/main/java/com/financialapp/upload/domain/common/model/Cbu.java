package com.financialapp.upload.domain.common.model;

import com.financialapp.upload.domain.exception.InvalidCbuException;

import java.util.regex.Pattern;

public record Cbu(String value) {

    private static final Pattern CBU_PATTERN = Pattern.compile("^\\d{22}$");

    public Cbu {
        if (value == null || !CBU_PATTERN.matcher(value).matches()) {
            throw new InvalidCbuException(value);
        }
    }

    @Override
    public String toString() {
        return value;
    }
}
