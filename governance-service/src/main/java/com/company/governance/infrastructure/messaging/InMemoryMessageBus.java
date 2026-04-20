package com.company.governance.infrastructure.messaging;

import com.company.governance.domain.messaging.entity.MessageEnvelope;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public class InMemoryMessageBus {

    private final Map<String, ConcurrentLinkedQueue<MessageEnvelope>> topicQueues =
        new ConcurrentHashMap<String, ConcurrentLinkedQueue<MessageEnvelope>>();

    public void offer(MessageEnvelope messageEnvelope) {
        queueOf(messageEnvelope.getTopic()).offer(messageEnvelope);
    }

    public ConcurrentLinkedQueue<MessageEnvelope> queueOf(String topic) {
        ConcurrentLinkedQueue<MessageEnvelope> queue = topicQueues.get(topic);
        if (queue == null) {
            ConcurrentLinkedQueue<MessageEnvelope> created = new ConcurrentLinkedQueue<MessageEnvelope>();
            ConcurrentLinkedQueue<MessageEnvelope> existing = topicQueues.putIfAbsent(topic, created);
            queue = existing == null ? created : existing;
        }
        return queue;
    }
}
