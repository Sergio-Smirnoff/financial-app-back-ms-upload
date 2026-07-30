package com.financialapp.upload.domain.exception;

public class InvalidFileHashException extends DomainException {

    public InvalidFileHashException(String value) {
        super("fileHash must be a 64-char lowercase hex SHA-256 string, got: " + value);
    }
}
