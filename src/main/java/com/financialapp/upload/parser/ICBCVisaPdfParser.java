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
    // Regex for: 07.03.26 006756* EMOVA SUBTE 1.363,00
    private static final Pattern TX_PATTERN = Pattern.compile("(\\d{2}\\.\\d{2}\\.\\d{2})\\s+(\\d{6}\\*?)\\s+(.*?)\\s+([\\d\\.]+?(,\\d{2})?)");

    @Override
    public List<ParsedTransaction> parse(InputStream is, Map<String, String> context) {
        List<ParsedTransaction> transactions = new ArrayList<>();
        try (PDDocument document = Loader.loadPDF(is.readAllBytes())) {
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(document);

            String[] lines = text.split("\\r?\\n");
            for (String line : lines) {
                Matcher matcher = TX_PATTERN.matcher(line);
                if (matcher.find()) {
                    try {
                        LocalDate date = LocalDate.parse(matcher.group(1), DATE_FORMATTER);
                        String description = matcher.group(3).trim();
                        String amountStr = matcher.group(4).replace(".", "").replace(",", ".");
                        BigDecimal amount = new BigDecimal(amountStr);

                        transactions.add(ParsedTransaction.builder()
                                .date(date)
                                .description(description)
                                .amount(amount)
                                .currency("ARS") // Default for now, can be improved to detect USD column
                                .type(TransactionType.EXPENSE)
                                .build());
                    } catch (Exception e) {
                        log.warn("Failed to parse PDF line: {}. Error: {}", line, e.getMessage());
                    }
                }
            }
        } catch (IOException e) {
            log.error("Error reading PDF file", e);
        }
        return transactions;
    }
}
