package com.financialapp.upload.domain.exception;

public class DuplicateImportException extends DomainException {

    public DuplicateImportException(String fileHash) {
        super("An active import run already exists for file hash: " + fileHash);
    }
}
