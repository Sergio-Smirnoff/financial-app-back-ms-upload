package com.financialapp.upload.application.importrun;

import com.financialapp.upload.application.importrun.impl.UndoImportUseCaseImpl;
import com.financialapp.upload.domain.common.model.*;
import com.financialapp.upload.domain.exception.ImportAlreadyUndoneException;
import com.financialapp.upload.domain.exception.ImportRunNotFoundException;
import com.financialapp.upload.domain.gateway.TransactionRecorderPort;
import com.financialapp.upload.domain.model.importrun.*;
import com.financialapp.upload.domain.repository.ImportRunRepository;
import com.financialapp.upload.domain.usecase.importrun.UndoImport;
import com.financialapp.upload.domain.usecase.importrun.command.UndoImportCommand;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UndoImportUseCaseTest {

    @Mock
    private ImportRunRepository importRunRepository;
    @Mock
    private TransactionRecorderPort transactionRecorderPort;

    private UndoImportUseCaseImpl useCase;

    private final UserId userId = new UserId(1L);
    private final ImportRunId runId = new ImportRunId(10L);
    private final BankNumber bankNumber = new BankNumber("011");
    private final Cbu cbu = new Cbu("0110000000000000000001");
    private final FileHash fileHash = new FileHash("9f86d081884c7d659a2feaa0c55ad015a3bf4f1b2b0b822cd15d6c15b0f00a08");
    private final DateRange period = new DateRange(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31));

    @BeforeEach
    void setUp() {
        useCase = new UndoImportUseCaseImpl(importRunRepository, transactionRecorderPort);
    }

    @Test
    void shouldUndoCompletedImportRun() {
        ImportRun run = ImportRun.create(runId, userId, bankNumber, cbu, fileHash, period, LocalDateTime.now())
                .markCompleted(List.of(101L, 102L), 0, null);

        when(importRunRepository.findByIdOwnedBy(runId, userId)).thenReturn(Optional.of(run));

        UndoImport.UndoResult result = useCase.execute(new UndoImportCommand(userId, runId));

        assertThat(result.deletedCount()).isEqualTo(2);
        assertThat(result.skippedCount()).isZero();
        verify(transactionRecorderPort).deleteTransaction(1L, 101L);
        verify(transactionRecorderPort).deleteTransaction(1L, 102L);
        verify(importRunRepository).save(argThat(r -> r.status() == ImportRunStatus.UNDONE));
    }

    @Test
    void shouldHandleSkippedTransactionWhenDeleteFails() {
        ImportRun run = ImportRun.create(runId, userId, bankNumber, cbu, fileHash, period, LocalDateTime.now())
                .markCompleted(List.of(101L, 102L), 0, null);

        when(importRunRepository.findByIdOwnedBy(runId, userId)).thenReturn(Optional.of(run));
        doThrow(new RuntimeException("Transaction not found")).when(transactionRecorderPort).deleteTransaction(1L, 102L);

        UndoImport.UndoResult result = useCase.execute(new UndoImportCommand(userId, runId));

        assertThat(result.deletedCount()).isEqualTo(1);
        assertThat(result.skippedCount()).isEqualTo(1);
        assertThat(result.skippedTransactionIds()).containsExactly(102L);
        verify(importRunRepository).save(argThat(r -> r.status() == ImportRunStatus.UNDONE));
    }

    @Test
    void shouldRejectDoubleUndo() {
        ImportRun undoneRun = ImportRun.create(runId, userId, bankNumber, cbu, fileHash, period, LocalDateTime.now())
                .markCompleted(List.of(101L), 0, null)
                .undo();

        when(importRunRepository.findByIdOwnedBy(runId, userId)).thenReturn(Optional.of(undoneRun));

        assertThatThrownBy(() -> useCase.execute(new UndoImportCommand(userId, runId)))
                .isInstanceOf(ImportAlreadyUndoneException.class);
    }

    @Test
    void shouldThrowNotFoundWhenRunNotOwnedByUser() {
        when(importRunRepository.findByIdOwnedBy(runId, userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(new UndoImportCommand(userId, runId)))
                .isInstanceOf(ImportRunNotFoundException.class);
    }
}
