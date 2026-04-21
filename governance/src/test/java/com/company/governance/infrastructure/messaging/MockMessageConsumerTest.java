package com.company.governance.infrastructure.messaging;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.company.governance.domain.messaging.MessageHandler;
import com.company.governance.domain.messaging.entity.MessageEnvelope;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class MockMessageConsumerTest {

    @Test
    void shouldDispatchMessagesInMockMode() {
        InMemoryMessageBus inMemoryMessageBus = new InMemoryMessageBus();
        MessageHandlerRegistry messageHandlerRegistry = new MessageHandlerRegistry();
        MockMessageProducer mockMessageProducer = new MockMessageProducer(inMemoryMessageBus);
        MockMessageConsumer mockMessageConsumer = new MockMessageConsumer(inMemoryMessageBus, messageHandlerRegistry);
        AtomicInteger handledCount = new AtomicInteger();

        mockMessageConsumer.listen(GovernanceMessagingTopics.AUDIT_EVENT, new MessageHandler() {
            @Override
            public void handle(MessageEnvelope messageEnvelope) {
                handledCount.incrementAndGet();
                assertEquals("tenant-a", messageEnvelope.getKey());
                assertEquals("{\"status\":\"OK\"}", messageEnvelope.getMessage());
            }
        });

        mockMessageProducer.send(
            GovernanceMessagingTopics.AUDIT_EVENT,
            "tenant-a",
            "{\"status\":\"OK\"}",
            Collections.singletonMap("traceId", "trace-001")
        );

        int dispatched = mockMessageConsumer.dispatch(GovernanceMessagingTopics.AUDIT_EVENT, 10);

        assertEquals(1, dispatched);
        assertEquals(1, handledCount.get());
    }
}
