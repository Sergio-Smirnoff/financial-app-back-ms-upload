package com.financialapp.upload.domain.usecase.importrun.command;

import com.financialapp.upload.domain.common.model.UserId;
import com.financialapp.upload.domain.model.importrun.ImportRunId;

public record UndoImportCommand(
        UserId userId,
        ImportRunId importRunId
) {
}
