package com.financialapp.upload.domain.gateway;

import com.financialapp.upload.domain.event.DomainEvent;

import java.util.List;

public interface DomainEventPublisher {
    void publish(DomainEvent event);
    void publishAll(List<DomainEvent> events);
}
