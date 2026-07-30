package com.financialapp.upload.domain.usecase.importrun;

import com.financialapp.upload.domain.common.model.UserId;
import com.financialapp.upload.domain.model.importrun.ImportRun;

import java.util.List;

public interface ListImportRuns {

    List<ImportRun> execute(UserId userId);
}
