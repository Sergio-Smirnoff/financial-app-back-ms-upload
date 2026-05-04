package com.financialapp.upload.model.dto.request;

import lombok.Data;
import java.util.List;

@Data
public class ResolveRequest {
    private String sessionId;
    private List<String> keepIds;
}
