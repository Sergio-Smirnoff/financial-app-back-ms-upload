package com.financialapp.upload.domain.repository;

import com.financialapp.commons.core.domain.model.Cbu;
import com.financialapp.upload.domain.common.model.UserId;
import com.financialapp.upload.domain.model.importrun.FileHash;
import com.financialapp.upload.domain.model.importrun.ImportRun;
import com.financialapp.upload.domain.model.importrun.ImportRunId;

import java.util.List;
import java.util.Optional;

public interface ImportRunRepository {

    ImportRun save(ImportRun importRun);

    Optional<ImportRun> findById(ImportRunId id);

    Optional<ImportRun> findByIdOwnedBy(ImportRunId id, UserId userId);

    List<ImportRun> findByUser(UserId userId);

    boolean existsActiveByUserAndFileHash(UserId userId, FileHash fileHash);

    Optional<ImportRun> findByTransactionId(long transactionId);

    Optional<ImportRun> findLatestCompletedOrPartialByAccount(UserId userId, Cbu accountCbu);
}
