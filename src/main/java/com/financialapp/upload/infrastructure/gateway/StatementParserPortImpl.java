package com.financialapp.upload.infrastructure.gateway;

import com.financialapp.upload.domain.gateway.StatementParserPort;
import com.financialapp.upload.domain.model.mapping.ColumnMapping;
import com.financialapp.upload.model.dto.ParsedTransaction;
import com.financialapp.upload.model.enums.FileType;
import com.financialapp.upload.parser.GenericCsvParser;
import com.financialapp.upload.service.ParsingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.Collections;
import java.util.List;

@Component
@RequiredArgsConstructor
public class StatementParserPortImpl implements StatementParserPort {

    private final ParsingService parsingService;
    private final GenericCsvParser genericCsvParser;

    @Override
    public List<ParsedTransaction> parse(InputStream is, FileType fileType, ColumnMapping columnMapping) {
        if (fileType == FileType.CSV && columnMapping != null) {
            return genericCsvParser.parse(is, columnMapping);
        }
        return parsingService.parse(is, fileType, Collections.emptyMap());
    }
}
