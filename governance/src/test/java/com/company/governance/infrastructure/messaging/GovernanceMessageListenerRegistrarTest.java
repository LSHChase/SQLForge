package com.company.governance.infrastructure.messaging;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.company.governance.domain.messaging.MessageConsumer;
import com.company.governance.domain.messaging.MessageHandler;
import com.company.governance.domain.messaging.entity.MessageEnvelope;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class GovernanceMessageListenerRegistrarTest {

    @Test
    void shouldRegisterAuditAndConfigListeners() {
        RecordingMessageConsumer messageConsumer = new RecordingMessageConsumer();

        new GovernanceMessageListenerRegistrar(messageConsumer);

        assertEquals(2, messageConsumer.handlers.size());
        assertNotNull(messageConsumer.handlers.get(GovernanceMessagingTopics.AUDIT_EVENT));
        assertNotNull(messageConsumer.handlers.get(GovernanceMessagingTopics.CONFIG_CHANGED));

        messageConsumer.handlers.get(GovernanceMessagingTopics.AUDIT_EVENT).handle(
            new MessageEnvelope(GovernanceMessagingTopics.AUDIT_EVENT, "tenant-a", "{\"status\":\"OK\"}", Collections.emptyMap())
        );
        messageConsumer.handlers.get(GovernanceMessagingTopics.CONFIG_CHANGED).handle(
            new MessageEnvelope(GovernanceMessagingTopics.CONFIG_CHANGED, "tenant-a", "{\"configType\":\"quota\"}", Collections.emptyMap())
        );
    }

    private static final class RecordingMessageConsumer implements MessageConsumer {

        private final Map<String, MessageHandler> handlers = new LinkedHashMap<String, MessageHandler>();

        @Override
        public List<MessageEnvelope> poll(String topic, int batchSize) {
            return Collections.emptyList();
        }

        @Override
        public void listen(String topic, MessageHandler handler) {
            handlers.put(topic, handler);
        }
    }
}
