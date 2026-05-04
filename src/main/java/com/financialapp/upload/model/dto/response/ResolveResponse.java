package com.financialapp.upload.model.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ResolveResponse {
    private int imported;
    private int skipped;
}
