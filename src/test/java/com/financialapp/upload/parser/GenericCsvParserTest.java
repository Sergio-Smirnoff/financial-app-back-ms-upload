package com.financialapp.upload.parser;

import com.financialapp.upload.model.dto.ParsedTransaction;
import com.financialapp.upload.model.enums.TransactionType;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class GenericCsvParserTest {

    private final GenericCsvParser parser = new GenericCsvParser();

    @Test
    void shouldParseCsvCorrectly() {
        String csv = "04/28/26,Compra / Venta de Titulo,302361.77,0.0,\n" +
                     "04/24/26,CREDITO POR RESCATE FCI,0.0,399988.79,";
        InputStream is = new ByteArrayInputStream(csv.getBytes());

        List<ParsedTransaction> result = parser.parse(is, new HashMap<>());

        assertThat(result).hasSize(2);
        
        assertThat(result.get(0).getDate()).isEqualTo(LocalDate.of(2026, 4, 28));
        assertThat(result.get(0).getDescription()).isEqualTo("Compra / Venta de Titulo");
        assertThat(result.get(0).getAmount()).isEqualByComparingTo("302361.77");
        assertThat(result.get(0).getType()).isEqualTo(TransactionType.EXPENSE);

        assertThat(result.get(1).getDate()).isEqualTo(LocalDate.of(2026, 4, 24));
        assertThat(result.get(1).getAmount()).isEqualByComparingTo("399988.79");
        assertThat(result.get(1).getType()).isEqualTo(TransactionType.INCOME);
    }
}
