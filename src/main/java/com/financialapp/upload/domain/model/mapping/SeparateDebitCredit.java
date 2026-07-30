package com.financialapp.upload.domain.model.mapping;

public record SeparateDebitCredit(int expenseColumn, int incomeColumn) implements AmountMapping {
}
