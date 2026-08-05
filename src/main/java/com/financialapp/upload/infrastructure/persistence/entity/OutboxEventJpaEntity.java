package com.financialapp.upload.infrastructure.persistence.entity;

import com.financialapp.commons.messaging.infrastructure.persistence.entity.OutboxRecordEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "outbox_event", schema = "upload")
@Getter
@Setter
public class OutboxEventJpaEntity extends OutboxRecordEntity {}
