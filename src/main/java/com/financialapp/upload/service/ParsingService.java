package com.financialapp.upload.service;

import com.financialapp.upload.model.dto.ParsedTransaction;
import com.financialapp.upload.parser.GenericCsvParser;
import com.financialapp.upload.parser.ICBCVisaPdfParser;
import com.financialapp.upload.parser.StatementParser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ParsingService {

    private final GenericCsvParser csvParser;
    private final ICBCVisaPdfParser icbcParser;

    public List<ParsedTransaction> parse(InputStream is, String originalName, Map<String, String> context) {
        StatementParser parser;
        if (originalName.toLowerCase().endsWith(".csv")) {
            parser = csvParser;
        } else if (originalName.toLowerCase().endsWith(".pdf")) {
            parser = icbcParser;
        } else {
            throw new IllegalArgumentException("Unsupported file format: " + originalName);
        }
        return parser.parse(is, context);
    }
}
