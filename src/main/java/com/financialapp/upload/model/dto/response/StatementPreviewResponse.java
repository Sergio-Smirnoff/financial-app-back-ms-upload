package com.financialapp.upload.model.dto.response;

import com.financialapp.upload.model.dto.ParsedTransaction;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class StatementPreviewResponse {
    private String tempKey;
    private String accountNumber;
    private List<ParsedTransaction> transactions;
    private BigDecimal totalAmount;
    private int count;
}
