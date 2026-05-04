package com.financialapp.upload.model.dto.request;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CardExpenseCreateRequest(
    String description,
    BigDecimal totalAmount,
    String currency,
    int totalInstallments,
    LocalDate firstDueDate
) {}
