package com.financialapp.upload.domain.usecase.importrun;

import com.financialapp.upload.domain.usecase.importrun.command.UndoImportCommand;

public interface UndoImport {

    UndoResult execute(UndoImportCommand command);

    record UndoResult(int deletedCount, int skippedCount, java.util.List<Long> skippedTransactionIds) {}
}
