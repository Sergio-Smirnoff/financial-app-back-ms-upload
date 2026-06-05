package com.financialapp.upload.exception;

import com.financialapp.commons.core.error.ErrorCategory;
import com.financialapp.commons.core.error.ErrorCode;

public enum DomainError implements ErrorCode {

    RESOURCE_NOT_FOUND(ErrorCategory.NOT_FOUND, "resource_not_found"),
    BUSINESS_RULE_VIOLATION(ErrorCategory.BAD_REQUEST, "business_rule_violation"),
    INVALID_FILE(ErrorCategory.BAD_REQUEST, "invalid_file"),
    PARSE_FAILED(ErrorCategory.UNPROCESSABLE, "parse_failed"),
    FILE_TOO_LARGE(ErrorCategory.BAD_REQUEST, "file_too_large"),
    DOWNSTREAM_ERROR(ErrorCategory.INTERNAL_SERVER_ERROR, "downstream_error"),
    INTERNAL_ERROR(ErrorCategory.INTERNAL_SERVER_ERROR, "internal_error");

    private final ErrorCategory category;
    private final String code;

    DomainError(ErrorCategory category, String code) {
        this.category = category;
        this.code = code;
    }

    @Override
    public ErrorCategory category() { return category; }

    @Override
    public String code() { return code; }
}
