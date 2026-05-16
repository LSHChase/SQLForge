package com.company.governance.infrastructure.messaging;

import com.company.governance.domain.messaging.MessageConsumer;
import com.company.governance.domain.messaging.MessageHandler;
import com.company.governance.domain.messaging.entity.MessageEnvelope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class GovernanceMessageListenerRegistrar {

    private static final Logger LOGGER = LoggerFactory.getLogger(GovernanceMessageListenerRegistrar.class);

    public GovernanceMessageListenerRegistrar(MessageConsumer messageConsumer) {
        messageConsumer.listen(GovernanceMessagingTopics.AUDIT_EVENT, new MessageHandler() {
            @Override
            public void handle(MessageEnvelope messageEnvelope) {
                LOGGER.info("已消费治理审计事件，topic={}, payload={}",
                    messageEnvelope.getTopic(), messageEnvelope.getMessage());
            }
        });
        messageConsumer.listen(GovernanceMessagingTopics.CONFIG_CHANGED, new MessageHandler() {
            @Override
            public void handle(MessageEnvelope messageEnvelope) {
                LOGGER.info("已消费治理配置事件，topic={}, payload={}",
                    messageEnvelope.getTopic(), messageEnvelope.getMessage());
            }
        });
    }
}
