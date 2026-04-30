package com.financialapp.upload.parser;

import com.financialapp.upload.model.dto.ParsedTransaction;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

public interface StatementParser {
    List<ParsedTransaction> parse(InputStream is, Map<String, String> context);
}
