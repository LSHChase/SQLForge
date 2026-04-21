package com.company.governance.infrastructure.messaging;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.company.governance.config.MessagingProperties;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class KafkaMessageProducerTest {

    @SuppressWarnings("unchecked")
    @Test
    void shouldSendKafkaMessageWithHeaders() {
        MessagingProperties messagingProperties = new MessagingProperties();
        messagingProperties.getKafka().setEnabled(true);
        messagingProperties.getKafka().setBootstrapServers("localhost:9092");
        Producer<String, String> producer = mock(Producer.class);
        Future<RecordMetadata> sendFuture = CompletableFuture.<RecordMetadata>completedFuture(null);
        when(producer.send(any(ProducerRecord.class)))
            .thenReturn(sendFuture);
        KafkaMessageProducer kafkaMessageProducer = new KafkaMessageProducer(
            messagingProperties,
            new KafkaMessageProducer.KafkaClientProducerFactory() {
                @Override
                public Producer<String, String> create(MessagingProperties ignored) {
                    return producer;
                }
            }
        );

        kafkaMessageProducer.send(
            GovernanceMessagingTopics.AUDIT_EVENT,
            "tenant-a",
            "{\"resultStatus\":\"SUCCESS\"}",
            Collections.singletonMap("traceId", "trace-001")
        );

        ArgumentCaptor<ProducerRecord> producerRecordCaptor = ArgumentCaptor.forClass(ProducerRecord.class);
        verify(producer).send(producerRecordCaptor.capture());
        ProducerRecord<String, String> producerRecord = producerRecordCaptor.getValue();
        Map<String, String> capturedHeaders = Collections.singletonMap(
            producerRecord.headers().iterator().next().key(),
            new String(producerRecord.headers().iterator().next().value(), StandardCharsets.UTF_8)
        );
        assertEquals(GovernanceMessagingTopics.AUDIT_EVENT, producerRecord.topic());
        assertEquals("tenant-a", producerRecord.key());
        assertEquals("{\"resultStatus\":\"SUCCESS\"}", producerRecord.value());
        assertEquals("trace-001", capturedHeaders.get("traceId"));
    }
}
