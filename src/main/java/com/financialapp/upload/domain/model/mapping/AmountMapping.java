package com.financialapp.upload.domain.model.mapping;

public sealed interface AmountMapping permits SingleSignedColumn, SeparateDebitCredit {
}
