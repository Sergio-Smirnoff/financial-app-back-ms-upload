package com.financialapp.upload.domain.exception;

public class InvalidBankNumberException extends DomainException {

    public InvalidBankNumberException(String value) {
        super("bankNumber must be exactly 3 digits, got: " + value);
    }
}
