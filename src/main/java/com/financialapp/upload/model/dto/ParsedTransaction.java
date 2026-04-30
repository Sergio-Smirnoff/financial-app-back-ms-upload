package com.financialapp.upload.model.dto;

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
public class ParsedTransaction {
    private LocalDate date;
    private String description;
    private BigDecimal amount;
    private String currency;
    private TransactionType type;
}
