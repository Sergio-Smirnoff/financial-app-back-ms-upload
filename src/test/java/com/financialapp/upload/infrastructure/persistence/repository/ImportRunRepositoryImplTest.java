package com.financialapp.upload.infrastructure.persistence.repository;
import com.financialapp.commons.core.domain.model.Cbu;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.financialapp.upload.domain.common.model.*;
import com.financialapp.upload.domain.model.importrun.*;
import com.financialapp.upload.infrastructure.persistence.entity.ImportRunJpaEntity;
import com.financialapp.upload.infrastructure.persistence.mapper.ImportRunPersistenceMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ImportRunRepositoryImplTest {

    private ImportRunJpaRepository jpaRepository;
    private ImportRunPersistenceMapper mapper;
    private ImportRunRepositoryImpl repository;

    private final UserId userId = new UserId(1L);
    private final BankNumber bankNumber = new BankNumber("011");
    private final Cbu cbu = new Cbu("0110000000000000000001");
    private final FileHash fileHash = new FileHash("9f86d081884c7d659a2feaa0c55ad015a3bf4f1b2b0b822cd15d6c15b0f00a08");
    private final DateRange period = new DateRange(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31));

    @BeforeEach
    void setUp() {
        jpaRepository = mock(ImportRunJpaRepository.class);
        mapper = new ImportRunPersistenceMapper(new ObjectMapper());
        repository = new ImportRunRepositoryImpl(jpaRepository, mapper);
    }

    @Test
    void shouldSaveAndReturnImportRun() {
        ReconciliationResult reconciliation = ReconciliationResult.of(
                Money.of(new BigDecimal("1000.00"), "ARS"),
                Money.of(new BigDecimal("1000.00"), "ARS")
        );

        ImportRun domain = ImportRun.create(null, userId, bankNumber, cbu, fileHash, period, LocalDateTime.now())
                .markCompleted(List.of(101L, 102L), 0, reconciliation);

        ImportRunJpaEntity savedEntity = mapper.toEntity(domain);
        savedEntity.setId(5L);

        when(jpaRepository.save(any(ImportRunJpaEntity.class))).thenReturn(savedEntity);

        ImportRun saved = repository.save(domain);

        assertThat(saved.id()).isEqualTo(new ImportRunId(5L));
        assertThat(saved.status()).isEqualTo(ImportRunStatus.COMPLETED);
        assertThat(saved.createdTransactionIds()).containsExactly(101L, 102L);
        assertThat(saved.reconciliation().matches()).isTrue();
    }

    @Test
    void shouldCheckActiveImportRunByFileHash() {
        when(jpaRepository.existsByUserIdAndFileHashAndStatusNot(1L, fileHash.value(), "UNDONE")).thenReturn(true);

        boolean exists = repository.existsActiveByUserAndFileHash(userId, fileHash);

        assertThat(exists).isTrue();
        verify(jpaRepository).existsByUserIdAndFileHashAndStatusNot(1L, fileHash.value(), "UNDONE");
    }

    @Test
    void shouldFindImportRunByTransactionId() {
        ImportRun domain = ImportRun.create(new ImportRunId(5L), userId, bankNumber, cbu, fileHash, period, LocalDateTime.now())
                .markCompleted(List.of(101L), 0, null);
        ImportRunJpaEntity entity = mapper.toEntity(domain);

        when(jpaRepository.findByTransactionId(101L)).thenReturn(Optional.of(entity));

        Optional<ImportRun> result = repository.findByTransactionId(101L);

        assertThat(result).isPresent();
        assertThat(result.get().id()).isEqualTo(new ImportRunId(5L));
    }
}
