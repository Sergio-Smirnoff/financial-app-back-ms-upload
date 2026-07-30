package com.financialapp.upload.parser;

import com.financialapp.upload.domain.model.mapping.ColumnMapping;
import com.financialapp.upload.domain.model.mapping.SeparateDebitCredit;
import com.financialapp.upload.domain.model.mapping.SingleSignedColumn;
import com.financialapp.upload.model.dto.ParsedTransaction;
import com.financialapp.upload.model.enums.TransactionType;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class GenericCsvParserTest {

    private final GenericCsvParser parser = new GenericCsvParser();

    @Test
    void shouldParseDebitAndCreditColumnsWhenDebitColAndCreditColContextKeysAreUsed() {
        String csvContent = """
                Date,Description,Debit,Credit
                2026-01-15,Supermarket,150.50,
                2026-01-16,Salary,,2500.00
                """;
        InputStream is = new ByteArrayInputStream(csvContent.getBytes(StandardCharsets.UTF_8));

        Map<String, String> context = new HashMap<>();
        context.put("dateCol", "0");
        context.put("descCol", "1");
        context.put("debitCol", "2");
        context.put("creditCol", "3");

        List<ParsedTransaction> result = parser.parse(is, context);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getType()).isEqualTo(TransactionType.EXPENSE);
        assertThat(result.get(0).getAmount()).isEqualByComparingTo("150.50");
        assertThat(result.get(1).getType()).isEqualTo(TransactionType.INCOME);
        assertThat(result.get(1).getAmount()).isEqualByComparingTo("2500.00");
    }

    @Test
    void shouldParseSingleSignedColumnWithNegativesAsExpensesAndPositivesAsIncomes() {
        String csvContent = """
                Fecha,Concepto,Monto
                2026-01-15,Restaurant,-85.00
                2026-01-16,Transfer In,400.00
                """;
        InputStream is = new ByteArrayInputStream(csvContent.getBytes(StandardCharsets.UTF_8));

        ColumnMapping mapping = new ColumnMapping(0, 1, new SingleSignedColumn(2), null, "yyyy-MM-dd");

        List<ParsedTransaction> result = parser.parse(is, mapping);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getType()).isEqualTo(TransactionType.EXPENSE);
        assertThat(result.get(0).getAmount()).isEqualByComparingTo("85.00");
        assertThat(result.get(1).getType()).isEqualTo(TransactionType.INCOME);
        assertThat(result.get(1).getAmount()).isEqualByComparingTo("400.00");
    }

    @Test
    void shouldExtractRunningBalanceFromBalanceColumn() {
        String csvContent = """
                Date,Desc,Debit,Credit,Balance
                2026-01-15,Supermarket,100.00,,900.00
                2026-01-16,Salary,,500.00,1400.00
                """;
        InputStream is = new ByteArrayInputStream(csvContent.getBytes(StandardCharsets.UTF_8));

        ColumnMapping mapping = new ColumnMapping(0, 1, new SeparateDebitCredit(2, 3), 4, "yyyy-MM-dd");

        List<ParsedTransaction> result = parser.parse(is, mapping);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getRunningBalance()).isEqualByComparingTo("900.00");
        assertThat(result.get(1).getRunningBalance()).isEqualByComparingTo("1400.00");
    }
}
