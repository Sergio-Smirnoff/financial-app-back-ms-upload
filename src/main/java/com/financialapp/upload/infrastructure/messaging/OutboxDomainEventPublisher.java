package com.financialapp.upload.infrastructure.messaging;

import com.financialapp.commons.messaging.infrastructure.messaging.relay.OutboxEventPublisher;
import com.financialapp.upload.domain.event.DomainEvent;
import com.financialapp.upload.domain.gateway.DomainEventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class OutboxDomainEventPublisher implements DomainEventPublisher {

    private final OutboxEventPublisher outboxEventPublisher;

    @Override
    public void publish(DomainEvent event) {
        outboxEventPublisher.publish(event);
    }

    @Override
    public void publishAll(List<DomainEvent> events) {
        for (DomainEvent event : events) {
            outboxEventPublisher.publish(event);
        }
    }
}
