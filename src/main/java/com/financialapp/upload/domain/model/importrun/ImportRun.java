package com.financialapp.upload.domain.model.importrun;

import com.financialapp.upload.domain.common.model.BankNumber;
import com.financialapp.commons.core.domain.model.Cbu;
import com.financialapp.upload.domain.common.model.DateRange;
import com.financialapp.upload.domain.common.model.UserId;
import com.financialapp.upload.domain.exception.ImportAlreadyUndoneException;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class ImportRun {

    private final ImportRunId id;
    private final UserId userId;
    private final BankNumber bankNumber;
    private final Cbu accountCbu;
    private final FileHash fileHash;
    private final DateRange period;
    private final ImportRunStatus status;
    private final List<Long> createdTransactionIds;
    private final int importedCount;
    private final int skippedCount;
    private final ReconciliationResult reconciliation;
    private final LocalDateTime lastStaleAlertAt;
    private final LocalDateTime createdAt;

    private ImportRun(
            ImportRunId id,
            UserId userId,
            BankNumber bankNumber,
            Cbu accountCbu,
            FileHash fileHash,
            DateRange period,
            ImportRunStatus status,
            List<Long> createdTransactionIds,
            int importedCount,
            int skippedCount,
            ReconciliationResult reconciliation,
            LocalDateTime lastStaleAlertAt,
            LocalDateTime createdAt
    ) {
        this.userId = Objects.requireNonNull(userId, "userId must not be null");
        this.bankNumber = Objects.requireNonNull(bankNumber, "bankNumber must not be null");
        this.accountCbu = Objects.requireNonNull(accountCbu, "accountCbu must not be null");
        this.fileHash = Objects.requireNonNull(fileHash, "fileHash must not be null");
        this.period = Objects.requireNonNull(period, "period must not be null");
        this.status = Objects.requireNonNull(status, "status must not be null");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");

        this.id = id;
        this.createdTransactionIds = createdTransactionIds != null
                ? List.copyOf(createdTransactionIds)
                : Collections.emptyList();
        this.importedCount = Math.max(0, importedCount);
        this.skippedCount = Math.max(0, skippedCount);
        this.reconciliation = reconciliation;
        this.lastStaleAlertAt = lastStaleAlertAt;
    }

    public static ImportRun create(
            ImportRunId id,
            UserId userId,
            BankNumber bankNumber,
            Cbu accountCbu,
            FileHash fileHash,
            DateRange period,
            LocalDateTime createdAt
    ) {
        return new ImportRun(
                id,
                userId,
                bankNumber,
                accountCbu,
                fileHash,
                period,
                ImportRunStatus.PENDING,
                Collections.emptyList(),
                0,
                0,
                null,
                null,
                createdAt != null ? createdAt : LocalDateTime.now()
        );
    }

    public static ImportRun reconstitute(
            ImportRunId id,
            UserId userId,
            BankNumber bankNumber,
            Cbu accountCbu,
            FileHash fileHash,
            DateRange period,
            ImportRunStatus status,
            List<Long> createdTransactionIds,
            int importedCount,
            int skippedCount,
            ReconciliationResult reconciliation,
            LocalDateTime lastStaleAlertAt,
            LocalDateTime createdAt
    ) {
        return new ImportRun(
                id,
                userId,
                bankNumber,
                accountCbu,
                fileHash,
                period,
                status,
                createdTransactionIds,
                importedCount,
                skippedCount,
                reconciliation,
                lastStaleAlertAt,
                createdAt
        );
    }

    public ImportRun markCompleted(List<Long> transactionIds, int skipped, ReconciliationResult reconciliationResult) {
        List<Long> safeIds = transactionIds != null ? transactionIds : Collections.emptyList();
        ImportRunStatus newStatus = (skipped == 0) ? ImportRunStatus.COMPLETED : ImportRunStatus.PARTIAL;
        return new ImportRun(
                this.id,
                this.userId,
                this.bankNumber,
                this.accountCbu,
                this.fileHash,
                this.period,
                newStatus,
                safeIds,
                safeIds.size(),
                skipped,
                reconciliationResult,
                this.lastStaleAlertAt,
                this.createdAt
        );
    }

    public ImportRun markFailed(String reason) {
        return new ImportRun(
                this.id,
                this.userId,
                this.bankNumber,
                this.accountCbu,
                this.fileHash,
                this.period,
                ImportRunStatus.FAILED,
                Collections.emptyList(),
                0,
                0,
                null,
                this.lastStaleAlertAt,
                this.createdAt
        );
    }

    public ImportRun undo() {
        if (this.status == ImportRunStatus.UNDONE) {
            throw new ImportAlreadyUndoneException(this.id != null ? this.id.value() : null);
        }
        return new ImportRun(
                this.id,
                this.userId,
                this.bankNumber,
                this.accountCbu,
                this.fileHash,
                this.period,
                ImportRunStatus.UNDONE,
                this.createdTransactionIds,
                this.importedCount,
                this.skippedCount,
                this.reconciliation,
                this.lastStaleAlertAt,
                this.createdAt
        );
    }

    public ImportRun markStaleAlerted(LocalDateTime now) {
        return new ImportRun(
                this.id,
                this.userId,
                this.bankNumber,
                this.accountCbu,
                this.fileHash,
                this.period,
                this.status,
                this.createdTransactionIds,
                this.importedCount,
                this.skippedCount,
                this.reconciliation,
                now,
                this.createdAt
        );
    }

    public ImportRunId id() { return id; }
    public UserId userId() { return userId; }
    public BankNumber bankNumber() { return bankNumber; }
    public Cbu accountCbu() { return accountCbu; }
    public FileHash fileHash() { return fileHash; }
    public DateRange period() { return period; }
    public ImportRunStatus status() { return status; }
    public List<Long> createdTransactionIds() { return createdTransactionIds; }
    public int importedCount() { return importedCount; }
    public int skippedCount() { return skippedCount; }
    public ReconciliationResult reconciliation() { return reconciliation; }
    public LocalDateTime lastStaleAlertAt() { return lastStaleAlertAt; }
    public LocalDateTime createdAt() { return createdAt; }
}
