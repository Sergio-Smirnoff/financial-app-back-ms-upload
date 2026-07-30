package com.financialapp.upload.domain.model.importrun;

import com.financialapp.upload.domain.common.model.Money;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ReconciliationResultTest {

    @Test
    void shouldReportMatchingBalancesWhenDiscrepancyIsZero() {
        Money statement = Money.of(new BigDecimal("1000.00"), "ARS");
        Money calculated = Money.of(new BigDecimal("1000.00"), "ARS");

        ReconciliationResult result = ReconciliationResult.of(statement, calculated);

        assertThat(result.matches()).isTrue();
        assertThat(result.discrepancy().amount()).isEqualByComparingTo("0.00");
    }

    @Test
    void shouldReportNonMatchingBalancesAndCalculateDiscrepancy() {
        Money statement = Money.of(new BigDecimal("1000.00"), "ARS");
        Money calculated = Money.of(new BigDecimal("950.00"), "ARS");

        ReconciliationResult result = ReconciliationResult.of(statement, calculated);

        assertThat(result.matches()).isFalse();
        assertThat(result.discrepancy().amount()).isEqualByComparingTo("50.00");
    }

    @Test
    void shouldForceMatchesFalseAndDiscrepancyNullWhenStatementBalanceIsNull() {
        Money calculated = Money.of(new BigDecimal("1000.00"), "ARS");

        ReconciliationResult result = ReconciliationResult.of(null, calculated);

        assertThat(result.statementBalance()).isNull();
        assertThat(result.matches()).isFalse();
        assertThat(result.discrepancy()).isNull();
    }

    @Test
    void shouldRejectMatchesTrueIfStatementBalanceIsNullInConstructor() {
        Money calculated = Money.of(new BigDecimal("1000.00"), "ARS");

        assertThatThrownBy(() -> new ReconciliationResult(null, calculated, true, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("matches must be false when statementBalance is null");
    }
}
