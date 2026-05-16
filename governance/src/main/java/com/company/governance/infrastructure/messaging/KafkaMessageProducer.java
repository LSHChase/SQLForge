package com.company.governance.infrastructure.messaging;

import com.company.governance.config.MessagingProperties;
import com.company.governance.domain.messaging.MessageProducer;
import com.company.sqlforge.common.constants.ErrorCodeConstants;
import com.company.sqlforge.common.exception.BizException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Future;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.util.StringUtils;

public class KafkaMessageProducer implements MessageProducer, org.springframework.beans.factory.DisposableBean {

    private static final Logger LOGGER = LoggerFactory.getLogger(KafkaMessageProducer.class);
    private static final Duration CLOSE_TIMEOUT = Duration.ofSeconds(5L);

    private final MessagingProperties messagingProperties;
    private final KafkaClientProducerFactory kafkaClientProducerFactory;
    private volatile Producer<String, String> producer;

    public KafkaMessageProducer(MessagingProperties messagingProperties) {
        this(messagingProperties, new DefaultKafkaClientProducerFactory());
    }

    KafkaMessageProducer(MessagingProperties messagingProperties,
                         KafkaClientProducerFactory kafkaClientProducerFactory) {
        this.messagingProperties = messagingProperties;
        this.kafkaClientProducerFactory = kafkaClientProducerFactory;
    }

    @Override
    public void send(String topic, String key, String message, Map<String, String> headers) {
        if (!StringUtils.hasText(topic) || !StringUtils.hasText(message)) {
            throw new BizException(
                ErrorCodeConstants.SYSTEM_INVALID_ARGUMENT,
                HttpStatus.BAD_REQUEST,
                "Kafka 消息 topic 和 payload 不能为空"
            );
        }
        ProducerRecord<String, String> producerRecord = new ProducerRecord<String, String>(topic, key, message);
        if (headers != null) {
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                if (entry.getKey() != null && entry.getValue() != null) {
                    producerRecord.headers().add(new RecordHeader(
                        entry.getKey(),
                        entry.getValue().getBytes(StandardCharsets.UTF_8)
                    ));
                }
            }
        }
        try {
            Future<RecordMetadata> future = getProducer().send(producerRecord);
            future.get();
            LOGGER.info("已发送 Kafka 消息，topic={}, partitionKey={}, messageLength={}",
                topic,
                key,
                message.length());
        } catch (Exception ex) {
            throw new BizException(
                ErrorCodeConstants.GOVERNANCE_SYSTEM_MESSAGE_ROUTE_INVALID,
                HttpStatus.INTERNAL_SERVER_ERROR,
                "发送 Kafka 消息失败",
                ex
            );
        }
    }

    @Override
    public void destroy() {
        Producer<String, String> currentProducer = producer;
        if (currentProducer != null) {
            currentProducer.flush();
            currentProducer.close(CLOSE_TIMEOUT);
        }
    }

    private Producer<String, String> getProducer() {
        Producer<String, String> currentProducer = producer;
        if (currentProducer != null) {
            return currentProducer;
        }
        synchronized (this) {
            if (producer == null) {
                producer = kafkaClientProducerFactory.create(messagingProperties);
            }
            return producer;
        }
    }

    interface KafkaClientProducerFactory {

        Producer<String, String> create(MessagingProperties messagingProperties);
    }

    static final class DefaultKafkaClientProducerFactory implements KafkaClientProducerFactory {

        @Override
        public Producer<String, String> create(MessagingProperties messagingProperties) {
            Properties properties = new Properties();
            properties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, messagingProperties.getKafka().getBootstrapServers());
            properties.put(ProducerConfig.ACKS_CONFIG, messagingProperties.getKafka().getProducer().getAcks());
            properties.put(ProducerConfig.RETRIES_CONFIG, messagingProperties.getKafka().getProducer().getRetries());
            properties.put(ProducerConfig.CLIENT_ID_CONFIG, messagingProperties.getKafka().getProducer().getClientId());
            properties.put(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG,
                messagingProperties.getKafka().getProducer().getRequestTimeoutMs());
            properties.put(ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG,
                messagingProperties.getKafka().getProducer().getDeliveryTimeoutMs());
            properties.put(ProducerConfig.MAX_BLOCK_MS_CONFIG,
                messagingProperties.getKafka().getProducer().getMaxBlockMs());
            properties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
            properties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
            KafkaClientPropertySupport.applyCommonKafkaProperties(properties, messagingProperties.getKafka());
            properties.putAll(messagingProperties.getKafka().getProducer().getProperties());
            return new KafkaProducer<String, String>(properties);
        }
    }
}
