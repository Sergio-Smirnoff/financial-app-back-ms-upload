package com.financialapp.upload.domain.gateway;

import com.financialapp.upload.domain.model.mapping.ColumnMapping;
import com.financialapp.upload.model.dto.ParsedTransaction;
import com.financialapp.upload.model.enums.FileType;

import java.io.InputStream;
import java.util.List;

public interface StatementParserPort {

    List<ParsedTransaction> parse(InputStream is, FileType fileType, ColumnMapping columnMapping);
}
