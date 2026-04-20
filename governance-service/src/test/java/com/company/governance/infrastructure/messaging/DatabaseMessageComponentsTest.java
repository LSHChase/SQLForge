package com.company.governance.infrastructure.messaging;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.company.governance.domain.message.entity.MessageQueueRecord;
import com.company.governance.domain.message.repository.MessageQueueRepository;
import com.company.governance.domain.messaging.MessageHandler;
import com.company.governance.domain.messaging.entity.MessageEnvelope;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class DatabaseMessageComponentsTest {

    @Test
    void shouldEnqueuePendingMessageThroughDatabaseProducer() {
        MessageQueueRepository repository = mock(MessageQueueRepository.class);
        DatabaseMessageProducer databaseMessageProducer = new DatabaseMessageProducer(repository);

        databaseMessageProducer.send(
            GovernanceMessagingTopics.CONFIG_CHANGED,
            "tenant-a",
            "{\"configType\":\"quota\"}",
            Collections.singletonMap("traceId", "trace-001")
        );

        verify(repository).enqueueMessage(any(MessageQueueRecord.class));
    }

    @Test
    void shouldDispatchPendingMessagesAndMarkConsumed() {
        MessageQueueRepository repository = mock(MessageQueueRepository.class);
        MessageHandlerRegistry registry = new MessageHandlerRegistry();
        DatabaseMessageConsumer databaseMessageConsumer = new DatabaseMessageConsumer(repository, registry);
        AtomicInteger handledCount = new AtomicInteger();

        MessageQueueRecord record = MessageQueueRecord.pending(
            GovernanceMessagingTopics.AUDIT_EVENT,
            "tenant-a",
            "{\"resultStatus\":\"SUCCESS\"}",
            "{\"traceId\":\"trace-001\"}"
        );
        record.setId(11L);

        registry.register(GovernanceMessagingTopics.AUDIT_EVENT, new MessageHandler() {
            @Override
            public void handle(MessageEnvelope messageEnvelope) {
                handledCount.incrementAndGet();
                assertEquals(GovernanceMessagingTopics.AUDIT_EVENT, messageEnvelope.getTopic());
            }
        });
        when(repository.pollPendingMessages(eq(GovernanceMessagingTopics.AUDIT_EVENT), eq(10)))
            .thenReturn(Collections.singletonList(record));

        int consumed = databaseMessageConsumer.dispatch(GovernanceMessagingTopics.AUDIT_EVENT, 10);

        assertEquals(1, consumed);
        assertEquals(1, handledCount.get());
        verify(repository).markMessageConsumed(11L);
    }
}
