package com.financialapp.upload.infrastructure.persistence.jpa;

import com.financialapp.upload.infrastructure.persistence.entity.OutboxEventJpaEntity;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OutboxEventJpaRepository extends JpaRepository<OutboxEventJpaEntity, Long> {
    List<OutboxEventJpaEntity> findBySentFalseOrderByIdAsc(Limit limit);
    Optional<OutboxEventJpaEntity> findByEventId(String eventId);
}
