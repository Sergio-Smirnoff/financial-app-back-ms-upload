package com.financialapp.upload.model.dto.request;

import com.financialapp.upload.model.enums.FileType;
import lombok.Data;

@Data
public class ConfirmRequest {
    private String tempKey;
    private FileType type;
    private ColumnMapping columnMapping;
    private String dateFormat;
    private Long accountId;
    private Long cardId;
    private Long arsAccountId;
    private Long usdAccountId;

    @Data
    public static class ColumnMapping {
        private int dateCol;
        private int descCol;
        private Integer expenseCol;
        private Integer incomeCol;
    }
}
