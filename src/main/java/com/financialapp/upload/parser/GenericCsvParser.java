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

    @Override
    public List<ParsedTransaction> parse(InputStream is, Map<String, String> context) {
        List<ParsedTransaction> transactions = new ArrayList<>();
        
        int dateCol = Integer.parseInt(context.getOrDefault("dateCol", "0"));
        int descCol = Integer.parseInt(context.getOrDefault("descCol", "1"));
        int debitCol = Integer.parseInt(context.getOrDefault("debitCol", "2"));
        int creditCol = Integer.parseInt(context.getOrDefault("creditCol", "3"));
        String dateFormat = context.getOrDefault("dateFormat", "MM/dd/yy");
        
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(dateFormat);

        try (CSVReader reader = new CSVReader(new InputStreamReader(is))) {
            String[] line;
            boolean firstLine = true;
            while ((line = reader.readNext()) != null) {
                // Skip header if it looks like headers (non-numeric in amount cols)
                if (firstLine) {
                    firstLine = false;
                    try {
                        new BigDecimal(line[debitCol].replaceAll("[^0-9.-]", ""));
                    } catch (Exception e) {
                        continue; // Likely header
                    }
                }

                try {
                    if (line.length <= Math.max(Math.max(dateCol, descCol), Math.max(debitCol, creditCol))) continue;

                    LocalDate date = LocalDate.parse(line[dateCol].trim(), formatter);
                    String description = line[descCol].trim();
                    
                    String debitStr = line[debitCol].replaceAll("[^0-9.-]", "");
                    String creditStr = line[creditCol].replaceAll("[^0-9.-]", "");
                    
                    BigDecimal debit = debitStr.isEmpty() ? BigDecimal.ZERO : new BigDecimal(debitStr);
                    BigDecimal credit = creditStr.isEmpty() ? BigDecimal.ZERO : new BigDecimal(creditStr);

                    BigDecimal amount;
                    TransactionType type;

                    if (credit.compareTo(BigDecimal.ZERO) > 0) {
                        amount = credit;
                        type = TransactionType.INCOME;
                    } else {
                        amount = debit.abs();
                        type = TransactionType.EXPENSE;
                    }

                    transactions.add(ParsedTransaction.builder()
                            .date(date)
                            .description(description)
                            .amount(amount)
                            .currency("ARS") // Default
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
