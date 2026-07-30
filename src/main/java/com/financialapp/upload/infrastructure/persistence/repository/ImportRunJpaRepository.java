package com.financialapp.upload.infrastructure.persistence.repository;

import com.financialapp.upload.infrastructure.persistence.entity.ImportRunJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ImportRunJpaRepository extends JpaRepository<ImportRunJpaEntity, Long> {

    List<ImportRunJpaEntity> findByUserIdOrderByCreatedAtDesc(Long userId);

    boolean existsByUserIdAndFileHashAndStatusNot(Long userId, String fileHash, String status);

    Optional<ImportRunJpaEntity> findByIdAndUserId(Long id, Long userId);

    @Query("SELECT r FROM ImportRunJpaEntity r JOIN r.transactionIds t WHERE t = :transactionId")
    Optional<ImportRunJpaEntity> findByTransactionId(@Param("transactionId") Long transactionId);

    Optional<ImportRunJpaEntity> findFirstByUserIdAndAccountCbuAndStatusInOrderByCreatedAtDesc(
            Long userId, String accountCbu, List<String> statuses);
}
