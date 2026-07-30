package com.financialapp.upload.domain.exception;

public class ImportAlreadyUndoneException extends DomainException {

    public ImportAlreadyUndoneException(Long importRunId) {
        super("Import run " + importRunId + " is already undone");
    }
}
