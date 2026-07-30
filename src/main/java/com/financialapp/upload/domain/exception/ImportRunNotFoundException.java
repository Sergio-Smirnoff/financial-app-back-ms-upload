package com.financialapp.upload.domain.exception;

public class ImportRunNotFoundException extends DomainException {

    public ImportRunNotFoundException(Long importRunId) {
        super("Import run not found: " + importRunId);
    }
}
