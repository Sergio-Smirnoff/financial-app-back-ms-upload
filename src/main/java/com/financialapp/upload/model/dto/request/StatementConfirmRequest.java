package com.financialapp.upload.model.dto.request;

import com.financialapp.upload.model.enums.FileType;
import lombok.Data;

import java.util.List;

@Data
public class StatementConfirmRequest {
    private String tempKey;
    private Long accountId;
    private String bankNumber;
    private String accountCbu;
    private FileType fileType;
    private List<TransactionMappingRequest> mappings;
}
