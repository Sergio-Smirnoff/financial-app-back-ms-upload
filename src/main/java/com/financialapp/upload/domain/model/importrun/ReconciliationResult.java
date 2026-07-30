package com.financialapp.upload.domain.model.importrun;

import com.financialapp.upload.domain.common.model.Money;

import java.util.Objects;

public record ReconciliationResult(
        Money statementBalance,
        Money calculatedBalance,
        boolean matches,
        Money discrepancy
) {

    public ReconciliationResult {
        Objects.requireNonNull(calculatedBalance, "calculatedBalance must not be null");
        if (statementBalance == null) {
            if (matches) {
                throw new IllegalArgumentException("matches must be false when statementBalance is null");
            }
            if (discrepancy != null) {
                throw new IllegalArgumentException("discrepancy must be null when statementBalance is null");
            }
        }
    }

    public static ReconciliationResult of(Money statementBalance, Money calculatedBalance) {
        Objects.requireNonNull(calculatedBalance, "calculatedBalance must not be null");
        if (statementBalance == null) {
            return new ReconciliationResult(null, calculatedBalance, false, null);
        }
        Money discrepancy = statementBalance.subtract(calculatedBalance);
        boolean matches = discrepancy.amount().compareTo(java.math.BigDecimal.ZERO) == 0;
        return new ReconciliationResult(statementBalance, calculatedBalance, matches, discrepancy);
    }
}
