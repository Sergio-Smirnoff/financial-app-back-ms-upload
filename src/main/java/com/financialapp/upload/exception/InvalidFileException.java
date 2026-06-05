package com.financialapp.upload.exception;

import com.financialapp.commons.core.error.DomainException;

public class InvalidFileException extends DomainException {

    public InvalidFileException(String message) {
        super(DomainError.INVALID_FILE, message);
    }
}
