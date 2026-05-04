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
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class GenericCsvParser implements StatementParser {

    private static final List<String> COMMON_PATTERNS = Arrays.asList(
            "yyyy-MM-dd",
            "dd/MM/yyyy",
            "MM/dd/yyyy",
            "dd/MM/yy",
            "MM/dd/yy",
            "yyyy/MM/dd",
            "dd-MM-yyyy"
    );

    @Override
    public List<ParsedTransaction> parse(InputStream is, Map<String, String> context) {
        List<ParsedTransaction> transactions = new ArrayList<>();
        
        int dateCol = Integer.parseInt(context.getOrDefault("dateCol", "0"));
        int descCol = Integer.parseInt(context.getOrDefault("descCol", "1"));
        Integer expenseCol = context.get("expenseCol") != null ? Integer.parseInt(context.get("expenseCol")) : null;
        Integer incomeCol = context.get("incomeCol") != null ? Integer.parseInt(context.get("incomeCol")) : null;
        String forcedFormat = context.get("dateFormat");
        
        try (CSVReader reader = new CSVReader(new InputStreamReader(is))) {
            String[] line;
            boolean firstLine = true;
            String detectedPattern = forcedFormat;

            while ((line = reader.readNext()) != null) {
                if (line.length <= Math.max(dateCol, descCol)) continue;

                if (firstLine) {
                    firstLine = false;
                    // Try to detect pattern if not forced
                    if (detectedPattern == null || "yyyy-MM-dd".equals(detectedPattern)) {
                        detectedPattern = detectPattern(line[dateCol].trim());
                        if (detectedPattern == null) continue; // Skip header or unparsable first line
                    } else {
                        // Check if it's a header even with forced format
                        if (!isParsable(line[dateCol].trim(), detectedPattern)) continue;
                    }
                }

                try {
                    String dateStr = line[dateCol].trim();
                    if (detectedPattern == null) {
                        detectedPattern = detectPattern(dateStr);
                    }
                    
                    if (detectedPattern == null) continue;

                    LocalDate date = LocalDate.parse(dateStr, DateTimeFormatter.ofPattern(detectedPattern));
                    String description = line[descCol].trim();
                    
                    BigDecimal expense = BigDecimal.ZERO;
                    BigDecimal income = BigDecimal.ZERO;

                    if (expenseCol != null && expenseCol < line.length) {
                        expense = parseAmount(line[expenseCol]);
                    }
                    if (incomeCol != null && incomeCol < line.length) {
                        income = parseAmount(line[incomeCol]);
                    }

                    BigDecimal amount;
                    TransactionType type;

                    if (expenseCol != null && incomeCol == null) {
                        if (expense.signum() < 0) {
                            amount = expense.abs();
                            type = TransactionType.INCOME;
                        } else {
                            amount = expense;
                            type = TransactionType.EXPENSE;
                        }
                    } else if (income.compareTo(BigDecimal.ZERO) != 0) {
                        amount = income;
                        type = TransactionType.INCOME;
                    } else {
                        amount = expense.abs();
                        type = TransactionType.EXPENSE;
                    }

                    if (amount.compareTo(BigDecimal.ZERO) != 0) {
                        transactions.add(ParsedTransaction.builder()
                                .date(date)
                                .description(description)
                                .amount(amount)
                                .currency("ARS")
                                .type(type)
                                .build());
                    }
                } catch (Exception e) {
                    log.warn("Failed to parse CSV line: {}. Error: {}", String.join(",", line), e.getMessage());
                }
            }
        } catch (IOException | CsvValidationException e) {
            log.error("Error reading CSV file", e);
        }
        return transactions;
    }

    private String detectPattern(String dateStr) {
        for (String pattern : COMMON_PATTERNS) {
            if (isParsable(dateStr, pattern)) {
                return pattern;
            }
        }
        return null;
    }

    private boolean isParsable(String dateStr, String pattern) {
        try {
            LocalDate.parse(dateStr, DateTimeFormatter.ofPattern(pattern));
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private BigDecimal parseAmount(String str) {
        if (str == null || str.trim().isEmpty()) return BigDecimal.ZERO;
        return new BigDecimal(str.trim().replaceAll("[^0-9.-]", ""));
    }
}
