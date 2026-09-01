package com.financialapp.upload.domain.exception;
import com.financialapp.commons.core.domain.model.Cbu;

public class InvalidCbuException extends DomainException {

    public InvalidCbuException(String value) {
        super("cbu must be exactly 22 digits, got: " + value);
    }
}
