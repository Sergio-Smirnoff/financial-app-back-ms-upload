package com.financialapp.upload.domain.usecase.importrun;

import com.financialapp.upload.domain.model.importrun.ImportRun;

import java.util.Optional;

public interface FindImportRunByTransaction {

    Optional<ImportRun> execute(long transactionId);
}
