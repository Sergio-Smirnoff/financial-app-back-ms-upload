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
public class TransactionMappingRequest {
    private LocalDate date;
    private String description;
    private BigDecimal amount;
    private String currency;
    private TransactionType type;
    private Long categoryId;
}
