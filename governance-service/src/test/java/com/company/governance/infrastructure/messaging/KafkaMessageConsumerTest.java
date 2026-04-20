package com.company.governance.infrastructure.messaging;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.company.governance.config.MessagingProperties;
import com.company.governance.domain.messaging.MessageHandler;
import com.company.governance.domain.messaging.entity.MessageEnvelope;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.junit.jupiter.api.Test;

class KafkaMessageConsumerTest {

    @Test
    void shouldDispatchKafkaRecordToRegisteredHandler() {
        MessagingProperties messagingProperties = new MessagingProperties();
        messagingProperties.getKafka().setEnabled(true);
        messagingProperties.getKafka().setBootstrapServers("localhost:9092");
        MessageHandlerRegistry messageHandlerRegistry = new MessageHandlerRegistry();
        KafkaMessageConsumer kafkaMessageConsumer = new KafkaMessageConsumer(
            messagingProperties,
            messageHandlerRegistry,
            new KafkaMessageConsumer.KafkaClientConsumerFactory() {
                @Override
                public org.apache.kafka.clients.consumer.Consumer<String, String> create(
                    MessagingProperties ignored,
                    String topic) {
                    return mockEmptyConsumer();
                }
            }
        );
        AtomicInteger handledCount = new AtomicInteger();

        messageHandlerRegistry.register(GovernanceMessagingTopics.AUDIT_EVENT, new MessageHandler() {
            @Override
            public void handle(MessageEnvelope messageEnvelope) {
                handledCount.incrementAndGet();
                assertEquals("tenant-a", messageEnvelope.getKey());
                assertEquals("trace-001", messageEnvelope.getHeaders().get("traceId"));
            }
        });

        RecordHeaders recordHeaders = new RecordHeaders();
        recordHeaders.add("traceId", "trace-001".getBytes(StandardCharsets.UTF_8));
        @SuppressWarnings("unchecked")
        ConsumerRecord<String, String> consumerRecord = mock(ConsumerRecord.class);
        when(consumerRecord.topic()).thenReturn(GovernanceMessagingTopics.AUDIT_EVENT);
        when(consumerRecord.key()).thenReturn("tenant-a");
        when(consumerRecord.value()).thenReturn("{\"status\":\"OK\"}");
        when(consumerRecord.headers()).thenReturn(recordHeaders);

        kafkaMessageConsumer.handleRecord(consumerRecord);

        assertEquals(1, handledCount.get());
    }

    @SuppressWarnings("unchecked")
    private org.apache.kafka.clients.consumer.Consumer<String, String> mockEmptyConsumer() {
        org.apache.kafka.clients.consumer.Consumer<String, String> consumer =
            org.mockito.Mockito.mock(org.apache.kafka.clients.consumer.Consumer.class);
        org.mockito.Mockito.when(consumer.poll(org.mockito.ArgumentMatchers.any(java.time.Duration.class)))
            .thenReturn(org.apache.kafka.clients.consumer.ConsumerRecords.empty());
        return consumer;
    }
}
