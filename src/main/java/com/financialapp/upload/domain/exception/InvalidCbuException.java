package com.financialapp.upload.domain.exception;

public class InvalidCbuException extends DomainException {

    public InvalidCbuException(String value) {
        super("cbu must be exactly 22 digits, got: " + value);
    }
}
