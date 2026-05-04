package com.financialapp.upload.model.dto.response;

import com.financialapp.upload.model.dto.ParsedTransaction;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@Builder
public class PreviewResponse {
    private String tempKey;
    private String fileHash;
    // CSV fields
    private List<String> headers;
    private List<List<String>> rows;
    // PDF/Parsed fields
    private List<ParsedTransaction> preview;
    private Integer totalCount;
    private Map<String, Integer> currencyCounts;
}
