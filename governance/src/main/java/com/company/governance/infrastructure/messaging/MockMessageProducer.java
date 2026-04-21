package com.company.governance.infrastructure.messaging;

import com.company.governance.domain.messaging.MessageProducer;
import com.company.governance.domain.messaging.entity.MessageEnvelope;
import java.util.Map;

public class MockMessageProducer implements MessageProducer {

    private final InMemoryMessageBus inMemoryMessageBus;

    public MockMessageProducer(InMemoryMessageBus inMemoryMessageBus) {
        this.inMemoryMessageBus = inMemoryMessageBus;
    }

    @Override
    public void send(String topic, String key, String message, Map<String, String> headers) {
        inMemoryMessageBus.offer(new MessageEnvelope(topic, key, message, headers));
    }
}
