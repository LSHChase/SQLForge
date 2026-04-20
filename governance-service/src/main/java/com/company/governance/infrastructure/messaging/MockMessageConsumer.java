package com.company.governance.infrastructure.messaging;

import com.company.governance.domain.messaging.MessageConsumer;
import com.company.governance.domain.messaging.MessageHandler;
import com.company.governance.domain.messaging.entity.MessageEnvelope;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

public class MockMessageConsumer implements MessageConsumer {

    private final InMemoryMessageBus inMemoryMessageBus;
    private final MessageHandlerRegistry messageHandlerRegistry;

    public MockMessageConsumer(InMemoryMessageBus inMemoryMessageBus,
                               MessageHandlerRegistry messageHandlerRegistry) {
        this.inMemoryMessageBus = inMemoryMessageBus;
        this.messageHandlerRegistry = messageHandlerRegistry;
    }

    @Override
    public List<MessageEnvelope> poll(String topic, int batchSize) {
        ConcurrentLinkedQueue<MessageEnvelope> queue = inMemoryMessageBus.queueOf(topic);
        List<MessageEnvelope> envelopes = new ArrayList<MessageEnvelope>(batchSize);
        while (envelopes.size() < batchSize) {
            MessageEnvelope envelope = queue.poll();
            if (envelope == null) {
                break;
            }
            envelopes.add(envelope);
        }
        return envelopes;
    }

    @Override
    public void listen(String topic, MessageHandler handler) {
        messageHandlerRegistry.register(topic, handler);
    }

    public int dispatch(String topic, int batchSize) {
        MessageHandler handler = messageHandlerRegistry.get(topic);
        if (handler == null) {
            return 0;
        }
        List<MessageEnvelope> envelopes = poll(topic, batchSize);
        for (MessageEnvelope envelope : envelopes) {
            handler.handle(envelope);
        }
        return envelopes.size();
    }
}
