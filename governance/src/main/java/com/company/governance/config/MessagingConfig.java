package com.company.governance.config;

import com.company.governance.domain.message.repository.MessageQueueRepository;
import com.company.governance.domain.messaging.MessageConsumer;
import com.company.governance.domain.messaging.MessageProducer;
import com.company.governance.infrastructure.messaging.DatabaseMessageConsumer;
import com.company.governance.infrastructure.messaging.DatabaseMessageProducer;
import com.company.governance.infrastructure.messaging.InMemoryMessageBus;
import com.company.governance.infrastructure.messaging.KafkaMessageConsumer;
import com.company.governance.infrastructure.messaging.KafkaMessageProducer;
import com.company.governance.infrastructure.messaging.MessageHandlerRegistry;
import com.company.governance.infrastructure.messaging.MockMessageConsumer;
import com.company.governance.infrastructure.messaging.MockMessageProducer;
import com.company.sqlforge.common.config.MessagingMode;
import com.company.sqlforge.common.constants.ErrorCodeConstants;
import com.company.sqlforge.common.exception.BizException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.util.StringUtils;

@Configuration
public class MessagingConfig {

    @Bean
    public MessageHandlerRegistry messageHandlerRegistry() {
        return new MessageHandlerRegistry();
    }

    @Bean
    public InMemoryMessageBus inMemoryMessageBus() {
        return new InMemoryMessageBus();
    }

    @Bean
    public MessageProducer messageProducer(MessagingProperties messagingProperties,
                                           MessageQueueRepository messageQueueRepository,
                                           InMemoryMessageBus inMemoryMessageBus) {
        if (messagingProperties.getMode() == MessagingMode.MOCK) {
            return new MockMessageProducer(inMemoryMessageBus);
        }
        if (messagingProperties.getMode() == MessagingMode.KAFKA) {
            validateKafkaProperties(messagingProperties);
            return new KafkaMessageProducer(messagingProperties);
        }
        return new DatabaseMessageProducer(messageQueueRepository);
    }

    @Bean
    public MessageConsumer messageConsumer(MessagingProperties messagingProperties,
                                           MessageQueueRepository messageQueueRepository,
                                           InMemoryMessageBus inMemoryMessageBus,
                                           MessageHandlerRegistry messageHandlerRegistry) {
        if (messagingProperties.getMode() == MessagingMode.MOCK) {
            return new MockMessageConsumer(inMemoryMessageBus, messageHandlerRegistry);
        }
        if (messagingProperties.getMode() == MessagingMode.KAFKA) {
            validateKafkaProperties(messagingProperties);
            return new KafkaMessageConsumer(messagingProperties, messageHandlerRegistry);
        }
        return new DatabaseMessageConsumer(messageQueueRepository, messageHandlerRegistry);
    }

    private void validateKafkaProperties(MessagingProperties messagingProperties) {
        if (!messagingProperties.getKafka().isEnabled()
            || !StringUtils.hasText(messagingProperties.getKafka().getBootstrapServers())) {
            throw new BizException(
                ErrorCodeConstants.SYSTEM_CONFIG_INVALID,
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Kafka messaging mode requires enabled bootstrap servers configuration"
            );
        }
    }
}
