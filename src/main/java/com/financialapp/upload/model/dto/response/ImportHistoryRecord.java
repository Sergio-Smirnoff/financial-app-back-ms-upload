package com.financialapp.upload.model.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ImportHistoryRecord {
    private Long id;
    private String originalName;
    private String fileType;
    private Long bankId;
    private Long accountId;
    private Long cardId;
    private int importedCount;
    private String importStatus;
    private String createdAt;
}
