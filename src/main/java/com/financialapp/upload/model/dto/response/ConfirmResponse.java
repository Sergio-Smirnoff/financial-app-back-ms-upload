package com.financialapp.upload.model.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ConfirmResponse {
    private int imported;
    private int skipped;
    private List<String> errors;
    private List<DuplicateItem> duplicates;
    private String sessionId;

    @Data
    @Builder
    public static class DuplicateItem {
        private String id;
        private String date;
        private String description;
        private double amount;
        private String currency;
    }
}
