package com.financialapp.upload.domain.usecase.importrun;

import com.financialapp.upload.domain.model.importrun.ImportRun;
import com.financialapp.upload.domain.usecase.importrun.command.ConfirmImportCommand;

public interface ConfirmImport {

    ImportRun execute(ConfirmImportCommand command);
}
