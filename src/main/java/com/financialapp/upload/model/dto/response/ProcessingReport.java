package com.financialapp.upload.model.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class ProcessingReport {
    private Long fileId;
    private String status;
    private int totalRows;
    private int successCount;
    private int errorCount;
    private List<RowError> errors;

    @Getter
    @Builder
    public static class RowError {
        private int rowNumber;
        private String description;
        private String errorMessage;
    }
}
