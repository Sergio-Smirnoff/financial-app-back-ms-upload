package com.financialapp.upload.model.dto.response;

import java.util.List;

public record BatchImportResponse(int imported, int skipped, List<String> errors) {}
