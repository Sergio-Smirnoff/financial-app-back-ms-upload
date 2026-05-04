package com.financialapp.upload.model.dto.request;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record CardExpenseImportRequest(
    Long arsAccountId,
    Long usdAccountId,
    List<ImportedExpense> expenses
) {
    public record ImportedExpense(
        String description,
        BigDecimal amount,
        String currency,
        LocalDate date
    ) {}
}
