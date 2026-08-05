package com.financialapp.upload.domain.usecase.importrun.command;

import com.financialapp.upload.domain.common.model.BankNumber;
import com.financialapp.commons.core.domain.model.Cbu;
import com.financialapp.upload.domain.common.model.UserId;
import com.financialapp.upload.domain.model.mapping.ColumnMapping;
import com.financialapp.upload.model.dto.request.TransactionMappingRequest;
import com.financialapp.upload.model.enums.FileType;

import java.util.List;

public record ConfirmImportCommand(
        UserId userId,
        String tempKey,
        FileType fileType,
        BankNumber bankNumber,
        Cbu accountCbu,
        Long accountId,
        ColumnMapping columnMapping,
        List<TransactionMappingRequest> manualMappings
) {
}
