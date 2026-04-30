package com.financialapp.upload.parser;

import com.financialapp.upload.model.dto.ParsedTransaction;
import com.financialapp.upload.model.enums.TransactionType;
import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class GenericCsvParser implements StatementParser {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MM/dd/yy");

    @Override
    public List<ParsedTransaction> parse(InputStream is, Map<String, String> context) {
        List<ParsedTransaction> transactions = new ArrayList<>();
        try (CSVReader reader = new CSVReader(new InputStreamReader(is))) {
            String[] line;
            while ((line = reader.readNext()) != null) {
                if (line.length < 4) continue;

                try {
                    LocalDate date = LocalDate.parse(line[0], DATE_FORMATTER);
                    String description = line[1];
                    BigDecimal debit = new BigDecimal(line[2]);
                    BigDecimal credit = new BigDecimal(line[3]);

                    BigDecimal amount;
                    TransactionType type;

                    if (credit.compareTo(BigDecimal.ZERO) > 0) {
                        amount = credit;
                        type = TransactionType.INCOME;
                    } else {
                        amount = debit;
                        type = TransactionType.EXPENSE;
                    }

                    transactions.add(ParsedTransaction.builder()
                            .date(date)
                            .description(description)
                            .amount(amount)
                            .currency("ARS") // Default for this CSV
                            .type(type)
                            .build());
                } catch (Exception e) {
                    log.warn("Failed to parse CSV line: {}. Error: {}", String.join(",", line), e.getMessage());
                }
            }
        } catch (IOException | CsvValidationException e) {
            log.error("Error reading CSV file", e);
        }
        return transactions;
    }
}
