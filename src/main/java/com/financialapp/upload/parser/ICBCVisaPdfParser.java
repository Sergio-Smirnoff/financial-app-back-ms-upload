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
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
public class ICBCVisaPdfParser implements StatementParser {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yy");
    
    // Pattern with voucher: 07.03.26 006756* EMOVA SUBTE 1.363,00
    private static final Pattern TX_VOUCHER_PATTERN = Pattern.compile("^\\s*(\\d{2}\\.\\d{2}\\.\\d{2})\\s+(\\d{6}\\*?)\\s+(.+?)\\s+([\\d\\.]+?(,\\d{2}))?$");

    // Pattern without voucher (taxes, etc): 07.03.26 IVA CONSUMO 21% 286,23
    private static final Pattern TX_NO_VOUCHER_PATTERN = Pattern.compile("^\\s*(\\d{2}\\.\\d{2}\\.\\d{2})\\s+(.+?)\\s+([\\d\\.]+?(,\\d{2}))\\s+([\\d\\.,]+|0,00)$");

    @Override
    public List<ParsedTransaction> parse(InputStream is, Map<String, String> context) {
        List<ParsedTransaction> transactions = new ArrayList<>();
        try (PDDocument document = Loader.loadPDF(is.readAllBytes())) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            String text = stripper.getText(document);

            boolean inTransactionSection = false;
            String[] lines = text.split("\\r?\\n");

            for (String line : lines) {
                if (line.contains("DETALLE DE TRANSACCION") || line.contains("DETALLE DE CONSUMOS") || line.contains("DETALLE DE MOVIMIENTOS")) {
                    inTransactionSection = true;
                    continue;
                }
                if (!inTransactionSection) continue;
                
                // Stop when we reach the summary/totals section
                if (line.contains("SALDO ACTUAL") || line.contains("PAGO MINIMO") || line.contains("TOTAL RESUMEN ANTERIOR")) {
                    inTransactionSection = false;
                    continue;
                }

                Matcher voucherMatcher = TX_VOUCHER_PATTERN.matcher(line);
                if (voucherMatcher.find()) {
                    parseLine(voucherMatcher.group(1), voucherMatcher.group(3), voucherMatcher.group(4), null, transactions);
                    continue;
                }

                Matcher noVoucherMatcher = TX_NO_VOUCHER_PATTERN.matcher(line);
                if (noVoucherMatcher.find()) {
                    parseLine(noVoucherMatcher.group(1), noVoucherMatcher.group(2), noVoucherMatcher.group(3), noVoucherMatcher.group(5), transactions);
                } else if (!line.isBlank() && line.matches(".*\\d{2}\\.\\d{2}\\.\\d{2}.*") && !line.contains("VENCIMIENTO") && !line.contains("CIERRE")) {
                    log.warn("Unmatched Visa line: '{}'", line);
                }
            }
        } catch (IOException e) {
            log.error("Error reading PDF file", e);
        }
        return transactions;
    }
    private void parseLine(String dateStr, String desc, String arsAmountStr, String usdAmountStr, List<ParsedTransaction> transactions) {
        try {
            LocalDate date = LocalDate.parse(dateStr, DATE_FORMATTER);
            String description = desc.trim();
            
            String currency = "ARS";
            BigDecimal amount = BigDecimal.ZERO;

            // If USD amount is present and ARS is "0,00" (or similar), it's a USD transaction
            if (usdAmountStr != null && !usdAmountStr.equals("0,00") && (arsAmountStr == null || arsAmountStr.equals("0,00"))) {
                currency = "USD";
                amount = parseAmount(usdAmountStr);
            } else if (arsAmountStr != null) {
                amount = parseAmount(arsAmountStr);
            }

            if (amount.compareTo(BigDecimal.ZERO) != 0) {
                transactions.add(ParsedTransaction.builder()
                        .date(date)
                        .description(description)
                        .amount(amount)
                        .currency(currency)
                        .type(TransactionType.EXPENSE)
                        .build());
            }
        } catch (Exception e) {
            log.warn("Failed to parse line parts: {} / {}. Error: {}", desc, arsAmountStr, e.getMessage());
        }
    }

    private BigDecimal parseAmount(String amountStr) {
        return new BigDecimal(amountStr.replace(".", "").replace(",", "."));
    }
}
