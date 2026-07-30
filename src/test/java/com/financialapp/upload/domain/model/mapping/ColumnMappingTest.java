package com.financialapp.upload.domain.model.mapping;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ColumnMappingTest {

    @Test
    void shouldCreateColumnMappingWithSeparateDebitCredit() {
        AmountMapping amountMapping = new SeparateDebitCredit(2, 3);
        ColumnMapping mapping = new ColumnMapping(0, 1, amountMapping, 4, "yyyy-MM-dd");

        assertThat(mapping.dateCol()).isEqualTo(0);
        assertThat(mapping.descriptionCol()).isEqualTo(1);
        assertThat(mapping.amountMapping()).isEqualTo(amountMapping);
        assertThat(mapping.balanceCol()).isEqualTo(4);
        assertThat(mapping.dateFormat()).isEqualTo("yyyy-MM-dd");
    }

    @Test
    void shouldCreateColumnMappingWithSingleSignedColumn() {
        AmountMapping amountMapping = new SingleSignedColumn(2);
        ColumnMapping mapping = new ColumnMapping(0, 1, amountMapping, null, null);

        assertThat(mapping.amountMapping()).isEqualTo(amountMapping);
        assertThat(mapping.balanceCol()).isNull();
    }

    @Test
    void shouldRejectNullAmountMapping() {
        assertThatThrownBy(() -> new ColumnMapping(0, 1, null, null, null))
                .isInstanceOf(NullPointerException.class);
    }
}
