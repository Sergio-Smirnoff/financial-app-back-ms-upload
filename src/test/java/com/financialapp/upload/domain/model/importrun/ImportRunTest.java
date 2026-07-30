package com.financialapp.upload.domain.model.importrun;

import com.financialapp.upload.domain.common.model.BankNumber;
import com.financialapp.upload.domain.common.model.Cbu;
import com.financialapp.upload.domain.common.model.DateRange;
import com.financialapp.upload.domain.common.model.UserId;
import com.financialapp.upload.domain.exception.ImportAlreadyUndoneException;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ImportRunTest {

    private final UserId userId = new UserId(1L);
    private final BankNumber bankNumber = new BankNumber("011");
    private final Cbu cbu = new Cbu("0110000000000000000001");
    private final FileHash fileHash = new FileHash("9f86d081884c7d659a2feaa0c55ad015a3bf4f1b2b0b822cd15d6c15b0f00a08");
    private final DateRange period = new DateRange(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31));
    private final LocalDateTime now = LocalDateTime.now();

    @Test
    void shouldCreatePendingImportRun() {
        ImportRun run = ImportRun.create(null, userId, bankNumber, cbu, fileHash, period, now);

        assertThat(run.status()).isEqualTo(ImportRunStatus.PENDING);
        assertThat(run.createdTransactionIds()).isEmpty();
        assertThat(run.importedCount()).isZero();
        assertThat(run.skippedCount()).isZero();
    }

    @Test
    void shouldTransitionToCompletedWhenSkippedCountIsZero() {
        ImportRun run = ImportRun.create(new ImportRunId(10L), userId, bankNumber, cbu, fileHash, period, now);

        ImportRun completed = run.markCompleted(List.of(101L, 102L), 0, null);

        assertThat(completed.status()).isEqualTo(ImportRunStatus.COMPLETED);
        assertThat(completed.createdTransactionIds()).containsExactly(101L, 102L);
        assertThat(completed.importedCount()).isEqualTo(2);
        assertThat(completed.skippedCount()).isZero();
    }

    @Test
    void shouldTransitionToPartialWhenSkippedCountIsGreaterThanZero() {
        ImportRun run = ImportRun.create(new ImportRunId(10L), userId, bankNumber, cbu, fileHash, period, now);

        ImportRun partial = run.markCompleted(List.of(101L), 1, null);

        assertThat(partial.status()).isEqualTo(ImportRunStatus.PARTIAL);
        assertThat(partial.createdTransactionIds()).containsExactly(101L);
        assertThat(partial.importedCount()).isEqualTo(1);
        assertThat(partial.skippedCount()).isEqualTo(1);
    }

    @Test
    void shouldTransitionToFailed() {
        ImportRun run = ImportRun.create(new ImportRunId(10L), userId, bankNumber, cbu, fileHash, period, now);

        ImportRun failed = run.markFailed("Parser error");

        assertThat(failed.status()).isEqualTo(ImportRunStatus.FAILED);
        assertThat(failed.createdTransactionIds()).isEmpty();
    }

    @Test
    void shouldTransitionToUndoneAndRejectDoubleUndo() {
        ImportRun run = ImportRun.create(new ImportRunId(10L), userId, bankNumber, cbu, fileHash, period, now)
                .markCompleted(List.of(101L), 0, null);

        ImportRun undone = run.undo();
        assertThat(undone.status()).isEqualTo(ImportRunStatus.UNDONE);

        assertThatThrownBy(undone::undo)
                .isInstanceOf(ImportAlreadyUndoneException.class);
    }

    @Test
    void shouldDefensivelyCopyCreatedTransactionIds() {
        List<Long> mutableIds = new java.util.ArrayList<>(List.of(100L));
        ImportRun run = ImportRun.create(new ImportRunId(10L), userId, bankNumber, cbu, fileHash, period, now)
                .markCompleted(mutableIds, 0, null);

        mutableIds.add(200L);

        assertThat(run.createdTransactionIds()).containsExactly(100L);
    }
}
