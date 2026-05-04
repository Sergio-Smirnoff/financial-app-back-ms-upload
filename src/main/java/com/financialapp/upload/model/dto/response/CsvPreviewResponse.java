package com.financialapp.upload.model.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class CsvPreviewResponse {
    private String tempKey;
    private List<String> headers;
    private List<List<String>> rows;
}
