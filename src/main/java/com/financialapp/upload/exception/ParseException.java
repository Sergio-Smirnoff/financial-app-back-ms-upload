package com.financialapp.upload.exception;

import com.financialapp.commons.core.error.DomainException;

public class ParseException extends DomainException {

    public ParseException(String message) {
        super(DomainError.PARSE_FAILED, message);
    }

    public ParseException(String message, Throwable cause) {
        super(DomainError.PARSE_FAILED, message, cause);
    }
}
