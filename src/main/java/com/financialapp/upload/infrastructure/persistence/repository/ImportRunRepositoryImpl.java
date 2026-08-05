package com.financialapp.upload.infrastructure.persistence.repository;

import com.financialapp.commons.core.domain.model.Cbu;
import com.financialapp.upload.domain.common.model.UserId;
import com.financialapp.upload.domain.model.importrun.FileHash;
import com.financialapp.upload.domain.model.importrun.ImportRun;
import com.financialapp.upload.domain.model.importrun.ImportRunId;
import com.financialapp.upload.domain.model.importrun.ImportRunStatus;
import com.financialapp.upload.domain.repository.ImportRunRepository;
import com.financialapp.upload.infrastructure.persistence.entity.ImportRunJpaEntity;
import com.financialapp.upload.infrastructure.persistence.mapper.ImportRunPersistenceMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class ImportRunRepositoryImpl implements ImportRunRepository {

    private final ImportRunJpaRepository jpaRepository;
    private final ImportRunPersistenceMapper mapper;

    @Override
    public ImportRun save(ImportRun importRun) {
        ImportRunJpaEntity entity = mapper.toEntity(importRun);
        ImportRunJpaEntity saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<ImportRun> findById(ImportRunId id) {
        if (id == null || id.value() == null) return Optional.empty();
        return jpaRepository.findById(id.value()).map(mapper::toDomain);
    }

    @Override
    public Optional<ImportRun> findByIdOwnedBy(ImportRunId id, UserId userId) {
        if (id == null || id.value() == null || userId == null || userId.value() == null) return Optional.empty();
        return jpaRepository.findByIdAndUserId(id.value(), userId.value()).map(mapper::toDomain);
    }

    @Override
    public List<ImportRun> findByUser(UserId userId) {
        if (userId == null || userId.value() == null) return List.of();
        return jpaRepository.findByUserIdOrderByCreatedAtDesc(userId.value()).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public boolean existsActiveByUserAndFileHash(UserId userId, FileHash fileHash) {
        if (userId == null || fileHash == null) return false;
        return jpaRepository.existsByUserIdAndFileHashAndStatusNot(
                userId.value(), fileHash.value(), ImportRunStatus.UNDONE.name());
    }

    @Override
    public Optional<ImportRun> findByTransactionId(long transactionId) {
        return jpaRepository.findByTransactionId(transactionId).map(mapper::toDomain);
    }

    @Override
    public Optional<ImportRun> findLatestCompletedOrPartialByAccount(UserId userId, Cbu accountCbu) {
        if (userId == null || accountCbu == null) return Optional.empty();
        List<String> statuses = List.of(ImportRunStatus.COMPLETED.name(), ImportRunStatus.PARTIAL.name());
        return jpaRepository.findFirstByUserIdAndAccountCbuAndStatusInOrderByCreatedAtDesc(
                userId.value(), accountCbu.value(), statuses).map(mapper::toDomain);
    }
}
