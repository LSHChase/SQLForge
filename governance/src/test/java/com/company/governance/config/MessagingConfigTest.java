package com.company.governance.config;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.mock;

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
import com.company.sqlforge.common.exception.BizException;
import org.junit.jupiter.api.Test;

class MessagingConfigTest {

    @Test
    void shouldCreateSupportingBeans() {
        MessagingConfig config = new MessagingConfig();

        assertInstanceOf(MessageHandlerRegistry.class, config.messageHandlerRegistry());
        assertInstanceOf(InMemoryMessageBus.class, config.inMemoryMessageBus());
    }

    @Test
    void shouldSelectProducerByMessagingMode() {
        MessagingConfig config = new MessagingConfig();
        MessageQueueRepository repository = mock(MessageQueueRepository.class);
        InMemoryMessageBus bus = new InMemoryMessageBus();

        MessagingProperties mockProperties = new MessagingProperties();
        mockProperties.setMode(MessagingMode.MOCK);
        MessageProducer mockProducer = config.messageProducer(mockProperties, repository, bus);
        assertInstanceOf(MockMessageProducer.class, mockProducer);

        MessagingProperties kafkaProperties = new MessagingProperties();
        kafkaProperties.setMode(MessagingMode.KAFKA);
        kafkaProperties.getKafka().setEnabled(true);
        kafkaProperties.getKafka().setBootstrapServers("localhost:9092");
        MessageProducer kafkaProducer = config.messageProducer(kafkaProperties, repository, bus);
        assertInstanceOf(KafkaMessageProducer.class, kafkaProducer);

        MessagingProperties databaseProperties = new MessagingProperties();
        databaseProperties.setMode(MessagingMode.DATABASE);
        MessageProducer databaseProducer = config.messageProducer(databaseProperties, repository, bus);
        assertInstanceOf(DatabaseMessageProducer.class, databaseProducer);
    }

    @Test
    void shouldSelectConsumerByMessagingMode() {
        MessagingConfig config = new MessagingConfig();
        MessageQueueRepository repository = mock(MessageQueueRepository.class);
        InMemoryMessageBus bus = new InMemoryMessageBus();
        MessageHandlerRegistry registry = new MessageHandlerRegistry();

        MessagingProperties mockProperties = new MessagingProperties();
        mockProperties.setMode(MessagingMode.MOCK);
        MessageConsumer mockConsumer = config.messageConsumer(mockProperties, repository, bus, registry);
        assertInstanceOf(MockMessageConsumer.class, mockConsumer);

        MessagingProperties kafkaProperties = new MessagingProperties();
        kafkaProperties.setMode(MessagingMode.KAFKA);
        kafkaProperties.getKafka().setEnabled(true);
        kafkaProperties.getKafka().setBootstrapServers("localhost:9092");
        MessageConsumer kafkaConsumer = config.messageConsumer(kafkaProperties, repository, bus, registry);
        assertInstanceOf(KafkaMessageConsumer.class, kafkaConsumer);

        MessagingProperties databaseProperties = new MessagingProperties();
        databaseProperties.setMode(MessagingMode.DATABASE);
        MessageConsumer databaseConsumer = config.messageConsumer(databaseProperties, repository, bus, registry);
        assertInstanceOf(DatabaseMessageConsumer.class, databaseConsumer);
    }

    @Test
    void shouldRejectKafkaModeWithoutBootstrapServers() {
        MessagingConfig config = new MessagingConfig();
        MessageQueueRepository repository = mock(MessageQueueRepository.class);
        InMemoryMessageBus bus = new InMemoryMessageBus();
        MessagingProperties kafkaProperties = new MessagingProperties();
        kafkaProperties.setMode(MessagingMode.KAFKA);
        kafkaProperties.getKafka().setEnabled(true);

        assertThrows(BizException.class, () -> config.messageProducer(kafkaProperties, repository, bus));
    }

    @Test
    void shouldRejectKafkaSaslModeWithoutMechanismAndJaas() {
        MessagingConfig config = new MessagingConfig();
        MessageQueueRepository repository = mock(MessageQueueRepository.class);
        InMemoryMessageBus bus = new InMemoryMessageBus();
        MessagingProperties kafkaProperties = new MessagingProperties();
        kafkaProperties.setMode(MessagingMode.KAFKA);
        kafkaProperties.getKafka().setEnabled(true);
        kafkaProperties.getKafka().setBootstrapServers("localhost:9092");
        kafkaProperties.getKafka().setSecurityProtocol("SASL_SSL");

        assertThrows(BizException.class, () -> config.messageProducer(kafkaProperties, repository, bus));
    }

    @Test
    void shouldRejectKafkaSslModeWithoutTruststore() {
        MessagingConfig config = new MessagingConfig();
        MessageQueueRepository repository = mock(MessageQueueRepository.class);
        InMemoryMessageBus bus = new InMemoryMessageBus();
        MessagingProperties kafkaProperties = new MessagingProperties();
        kafkaProperties.setMode(MessagingMode.KAFKA);
        kafkaProperties.getKafka().setEnabled(true);
        kafkaProperties.getKafka().setBootstrapServers("localhost:9092");
        kafkaProperties.getKafka().setSecurityProtocol("SSL");

        assertThrows(BizException.class, () -> config.messageProducer(kafkaProperties, repository, bus));
    }

    @Test
    void shouldAcceptKafkaSslModeWhenSecurityParametersAreComplete() {
        MessagingConfig config = new MessagingConfig();
        MessageQueueRepository repository = mock(MessageQueueRepository.class);
        InMemoryMessageBus bus = new InMemoryMessageBus();
        MessagingProperties kafkaProperties = new MessagingProperties();
        kafkaProperties.setMode(MessagingMode.KAFKA);
        kafkaProperties.getKafka().setEnabled(true);
        kafkaProperties.getKafka().setBootstrapServers("localhost:9092");
        kafkaProperties.getKafka().setSecurityProtocol("SASL_SSL");
        kafkaProperties.getKafka().setSaslMechanism("PLAIN");
        kafkaProperties.getKafka().setSaslJaasConfig(
            "org.apache.kafka.common.security.plain.PlainLoginModule required username=\"u\" password=\"p\";"
        );
        kafkaProperties.getKafka().setSslTruststoreLocation("/tmp/sqlforge-kafka.truststore.jks");
        kafkaProperties.getKafka().setSslTruststorePassword("secret");

        assertDoesNotThrow(() -> config.messageProducer(kafkaProperties, repository, bus));
    }
}
