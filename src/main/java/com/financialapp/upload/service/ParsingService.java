package com.financialapp.upload.service;

import com.financialapp.upload.model.dto.ParsedTransaction;
import com.financialapp.upload.model.enums.FileType;
import com.financialapp.upload.parser.GenericCsvParser;
import com.financialapp.upload.parser.ICBCBankMovementsPdfParser;
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
    private final ICBCVisaPdfParser icbcVisaParser;
    private final ICBCBankMovementsPdfParser icbcBankParser;

    public List<ParsedTransaction> parse(InputStream is, FileType type, Map<String, String> context) {
        StatementParser parser = switch (type) {
            case CSV -> csvParser;
            case VISA_PDF -> icbcVisaParser;
            case BANK_PDF -> icbcBankParser;
        };
        return parser.parse(is, context);
    }
}
