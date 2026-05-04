package com.financialapp.upload.model.dto.response;

import com.financialapp.upload.model.enums.ImportStatus;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CsvImportResponse {
    private Long importId;
    private ImportStatus status;
    private int importedCount;
}
