package com.financialapp.upload.parser;

import com.financialapp.upload.model.dto.ParsedTransaction;
import com.financialapp.upload.model.enums.TransactionType;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
public class ICBCBankMovementsPdfParser implements StatementParser {

    private static final Pattern PERIOD_PATTERN = Pattern.compile("PERIODO\\s+(\\d{2}[-/]\\d{2}[-/]\\d{4})");
    // Flexible pattern: Date, then some columns, then up to 3 amounts
    // Handles 12-05 or 12/05/2025, and amounts like 1.500,00 or $ 1.500,00 or 1.500,00-
    private static final Pattern TX_PATTERN = Pattern.compile("^\\s*(\\d{2}[-/]\\d{2}(?:[-/]\\d{2,4})?)\\s+(.+?)\\s+([$ ]*[\\d\\.]+?,\\d{2}-?)(?:\\s+([$ ]*[\\d\\.]+?,\\d{2}-?))?(?:\\s+([$ ]*[\\d\\.]+?,\\d{2}-?))?$");

    @Override
    public List<ParsedTransaction> parse(InputStream is, Map<String, String> context) {
        List<ParsedTransaction> transactions = new ArrayList<>();
        try (PDDocument document = Loader.loadPDF(is.readAllBytes())) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            String text = stripper.getText(document);

            String[] lines = text.split("\\r?\\n");
            int baseYear = LocalDate.now().getYear();

            for (String line : lines) {
                if (line.isBlank()) continue;

                Matcher periodMatcher = PERIOD_PATTERN.matcher(line);
                if (periodMatcher.find()) {
                    String dateStr = periodMatcher.group(1);
                    baseYear = Integer.parseInt(dateStr.substring(dateStr.length() - 4));
                    continue;
                }

                if (line.contains("APERTURA") || line.contains("SALDO INICIAL") || line.contains("SALDO FINAL") || line.contains("SUBTOTAL") || line.contains("PAGINA")) continue;

                Matcher txMatcher = TX_PATTERN.matcher(line);
                if (txMatcher.find()) {
                    try {
                        String dateRaw = txMatcher.group(1).replace("/", "-");
                        int day = Integer.parseInt(dateRaw.substring(0, 2));
                        int month = Integer.parseInt(dateRaw.substring(3, 5));
                        int year = dateRaw.length() > 5 ? Integer.parseInt(dateRaw.substring(6)) : baseYear;
                        if (year < 100) year += 2000;
                        LocalDate date = LocalDate.of(year, month, day);

                        String description = txMatcher.group(2).trim();

                        // Pick the amount. Usually first column is Debit/Credit if 3 columns, or just Amount if 2.
                        String amountRaw = txMatcher.group(3).trim();

                        // If the first amount is very small (like a balance), we might need to check others.
                        // But for now let's use the first one and check sign.
                        boolean isNegative = amountRaw.endsWith("-");
                        String cleanAmount = amountRaw.replace("-", "").replace("$", "").replace(" ", "").replace(".", "").replace(",", ".");

                        BigDecimal amount = new BigDecimal(cleanAmount);
                        TransactionType type = isNegative ? TransactionType.EXPENSE : TransactionType.INCOME;

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
                        log.warn("Failed to parse movement line: '{}'. Error: {}", line, e.getMessage());
                    }
                } else if (line.matches(".*\\d{2}[-/]\\d{2}.*") && !line.contains("Hoja") && !line.contains("C.B.U.")) {
                     log.debug("Line ignored but contains date: '{}'", line);
                }
            }
        } catch (IOException e) {
            log.error("Error reading PDF file", e);
        }
        return transactions;
    }}
