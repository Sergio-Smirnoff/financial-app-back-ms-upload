package com.financialapp.upload.domain.exception;

public class ImportRowsModifiedException extends DomainException {

    public ImportRowsModifiedException(Long importRunId, String detail) {
        super("Import run " + importRunId + " cannot be undone because rows were modified: " + detail);
    }
}
