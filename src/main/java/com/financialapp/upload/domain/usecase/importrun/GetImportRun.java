package com.financialapp.upload.domain.usecase.importrun;

import com.financialapp.upload.domain.common.model.UserId;
import com.financialapp.upload.domain.model.importrun.ImportRun;
import com.financialapp.upload.domain.model.importrun.ImportRunId;

import java.util.Optional;

public interface GetImportRun {

    Optional<ImportRun> execute(UserId userId, ImportRunId id);
}
