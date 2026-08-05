package com.financialapp.upload.model.dto.request;
import com.financialapp.commons.core.domain.model.Cbu;

import com.financialapp.upload.model.enums.FileType;
import lombok.Data;

import java.util.List;

@Data
public class CsvConfirmRequest {
    private String tempKey;
    private int dateCol;
    private int descCol;
    private int debitCol;
    private int creditCol;
    private Integer montoCol;
    private Integer balanceCol;
    private String dateFormat;
    private Long accountId;
    private String bankNumber;
    private String accountCbu;
    private FileType fileType;
    private List<TransactionMappingRequest> mappings;
}
