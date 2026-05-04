package com.financialapp.upload.model.enums;

public enum ImportStatus {
    PENDING,
    COMPLETED,
    FAILED,
    IMPORTED, // Keeping old for compatibility if needed
    ALREADY_EXISTS,
    PARTIAL
}
