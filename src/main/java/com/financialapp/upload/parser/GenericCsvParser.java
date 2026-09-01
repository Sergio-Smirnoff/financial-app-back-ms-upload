package com.financialapp.upload.parser;

import com.financialapp.upload.domain.model.mapping.AmountMapping;
import com.financialapp.upload.domain.model.mapping.ColumnMapping;
import com.financialapp.upload.domain.model.mapping.SeparateDebitCredit;
import com.financialapp.upload.domain.model.mapping.SingleSignedColumn;
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
import java.util.*;

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

    public List<ParsedTransaction> parse(InputStream is, ColumnMapping mapping) {
        Map<String, String> context = new HashMap<>();
        context.put("dateCol", String.valueOf(mapping.dateCol()));
        context.put("descCol", String.valueOf(mapping.descriptionCol()));
        if (mapping.dateFormat() != null) {
            context.put("dateFormat", mapping.dateFormat());
        }
        if (mapping.balanceCol() != null) {
            context.put("balanceCol", String.valueOf(mapping.balanceCol()));
        }
        if (mapping.amountMapping() instanceof SingleSignedColumn s) {
            context.put("montoCol", String.valueOf(s.column()));
        } else if (mapping.amountMapping() instanceof SeparateDebitCredit d) {
            context.put("expenseCol", String.valueOf(d.expenseColumn()));
            context.put("debitCol", String.valueOf(d.expenseColumn()));
            context.put("incomeCol", String.valueOf(d.incomeColumn()));
            context.put("creditCol", String.valueOf(d.incomeColumn()));
        }
        return parse(is, context);
    }

    @Override
    public List<ParsedTransaction> parse(InputStream is, Map<String, String> context) {
        List<ParsedTransaction> transactions = new ArrayList<>();

        int dateCol = Integer.parseInt(context.getOrDefault("dateCol", "0"));
        int descCol = Integer.parseInt(context.getOrDefault("descCol", "1"));
        
        Integer expenseCol = getIntKey(context, "expenseCol", "debitCol");
        Integer incomeCol = getIntKey(context, "incomeCol", "creditCol");
        Integer montoCol = getIntKey(context, "montoCol", "signedCol", "singleSignedCol");
        Integer balanceCol = getIntKey(context, "balanceCol");

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

                    BigDecimal amount = BigDecimal.ZERO;
                    TransactionType type = TransactionType.EXPENSE;

                    if (montoCol != null && montoCol < line.length) {
                        BigDecimal val = parseAmount(line[montoCol]);
                        if (val.signum() < 0) {
                            amount = val.abs();
                            type = TransactionType.EXPENSE;
                        } else {
                            amount = val;
                            type = TransactionType.INCOME;
                        }
                    } else {
                        BigDecimal expense = BigDecimal.ZERO;
                        BigDecimal income = BigDecimal.ZERO;

                        if (expenseCol != null && expenseCol < line.length) {
                            expense = parseAmount(line[expenseCol]);
                        }
                        if (incomeCol != null && incomeCol < line.length) {
                            income = parseAmount(line[incomeCol]);
                        }

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
                    }

                    BigDecimal runningBalance = null;
                    if (balanceCol != null && balanceCol < line.length) {
                        runningBalance = parseAmount(line[balanceCol]);
                    }

                    if (amount.compareTo(BigDecimal.ZERO) != 0) {
                        transactions.add(ParsedTransaction.builder()
                                .date(date)
                                .description(description)
                                .amount(amount)
                                .currency("ARS")
                                .type(type)
                                .runningBalance(runningBalance)
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

    private Integer getIntKey(Map<String, String> context, String... keys) {
        for (String key : keys) {
            String val = context.get(key);
            if (val != null && !val.isBlank()) {
                try {
                    return Integer.parseInt(val.trim());
                } catch (NumberFormatException ignored) {
                }
            }
        }
        return null;
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
