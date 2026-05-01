package com.financialapp.upload.model.dto.request;

import com.financialapp.upload.model.enums.TransactionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionRequest {
    private TransactionType type;
    private BigDecimal amount;
    private Long accountId;
    private String currency;
    private Long categoryId;
    private String description;
    private LocalDate date;
}
