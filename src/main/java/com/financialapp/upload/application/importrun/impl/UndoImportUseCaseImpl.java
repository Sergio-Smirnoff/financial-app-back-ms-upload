package com.financialapp.upload.application.importrun.impl;

import com.financialapp.upload.domain.exception.ImportRunNotFoundException;
import com.financialapp.upload.domain.gateway.TransactionRecorderPort;
import com.financialapp.upload.domain.model.importrun.ImportRun;
import com.financialapp.upload.domain.repository.ImportRunRepository;
import com.financialapp.upload.domain.usecase.importrun.UndoImport;
import com.financialapp.upload.domain.usecase.importrun.command.UndoImportCommand;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class UndoImportUseCaseImpl implements UndoImport {

    private final ImportRunRepository importRunRepository;
    private final TransactionRecorderPort transactionRecorderPort;

    @Override
    @Transactional
    public UndoResult execute(UndoImportCommand command) {
        ImportRun run = importRunRepository.findByIdOwnedBy(command.importRunId(), command.userId())
                .orElseThrow(() -> new ImportRunNotFoundException(command.importRunId().value()));

        // Will throw ImportAlreadyUndoneException if already UNDONE
        ImportRun undoneRun = run.undo();

        int deletedCount = 0;
        int skippedCount = 0;
        List<Long> skippedTransactionIds = new ArrayList<>();

        for (Long txId : run.createdTransactionIds()) {
            try {
                transactionRecorderPort.deleteTransaction(command.userId().value(), txId);
                deletedCount++;
            } catch (Exception e) {
                log.warn("Failed to delete transaction {} during undo of run {}: {}", txId, run.id().value(), e.getMessage());
                skippedCount++;
                skippedTransactionIds.add(txId);
            }
        }

        importRunRepository.save(undoneRun);

        return new UndoResult(deletedCount, skippedCount, skippedTransactionIds);
    }
}
