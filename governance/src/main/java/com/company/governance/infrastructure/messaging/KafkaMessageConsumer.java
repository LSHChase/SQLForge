package com.company.governance.infrastructure.messaging;

import com.company.governance.config.MessagingProperties;
import com.company.governance.domain.messaging.MessageConsumer;
import com.company.governance.domain.messaging.MessageHandler;
import com.company.governance.domain.messaging.entity.MessageEnvelope;
import com.company.sqlforge.common.constants.ErrorCodeConstants;
import com.company.sqlforge.common.exception.BizException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicBoolean;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;

public class KafkaMessageConsumer implements MessageConsumer, org.springframework.beans.factory.DisposableBean {

    private static final Logger LOGGER = LoggerFactory.getLogger(KafkaMessageConsumer.class);
    private static final Duration CLOSE_TIMEOUT = Duration.ofSeconds(5L);

    private final MessagingProperties messagingProperties;
    private final MessageHandlerRegistry messageHandlerRegistry;
    private final KafkaClientConsumerFactory kafkaClientConsumerFactory;
    private final ExecutorService listenerExecutor;
    private final ConcurrentMap<String, Future<?>> topicListeners = new ConcurrentHashMap<String, Future<?>>();
    private final AtomicBoolean running = new AtomicBoolean(true);

    public KafkaMessageConsumer(MessagingProperties messagingProperties,
                                MessageHandlerRegistry messageHandlerRegistry) {
        this(messagingProperties, messageHandlerRegistry, new DefaultKafkaClientConsumerFactory());
    }

    KafkaMessageConsumer(MessagingProperties messagingProperties,
                         MessageHandlerRegistry messageHandlerRegistry,
                         KafkaClientConsumerFactory kafkaClientConsumerFactory) {
        this.messagingProperties = messagingProperties;
        this.messageHandlerRegistry = messageHandlerRegistry;
        this.kafkaClientConsumerFactory = kafkaClientConsumerFactory;
        this.listenerExecutor = Executors.newCachedThreadPool(new ThreadFactory() {
            @Override
            public Thread newThread(Runnable runnable) {
                Thread thread = new Thread(runnable);
                thread.setName("governance-kafka-consumer");
                thread.setDaemon(true);
                return thread;
            }
        });
    }

    @Override
    public List<MessageEnvelope> poll(String topic, int batchSize) {
        Consumer<String, String> consumer = kafkaClientConsumerFactory.create(messagingProperties, topic + "-poll");
        try {
            consumer.subscribe(Collections.singletonList(topic));
            ConsumerRecords<String, String> records = consumer.poll(currentPollTimeout());
            List<MessageEnvelope> envelopes = new ArrayList<MessageEnvelope>(Math.min(records.count(), batchSize));
            for (ConsumerRecord<String, String> record : records) {
                if (envelopes.size() >= batchSize) {
                    break;
                }
                envelopes.add(toEnvelope(record));
            }
            if (!envelopes.isEmpty()) {
                consumer.commitSync();
            }
            return envelopes;
        } catch (Exception ex) {
            throw new BizException(
                ErrorCodeConstants.GOVERNANCE_SYSTEM_MESSAGE_ROUTE_INVALID,
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Failed to poll Kafka messages",
                ex
            );
        } finally {
            consumer.close(CLOSE_TIMEOUT);
        }
    }

    @Override
    public void listen(String topic, MessageHandler handler) {
        LOGGER.info("Kafka listener registered for topic={}", topic);
        messageHandlerRegistry.register(topic, handler);
        topicListeners.computeIfAbsent(topic, new java.util.function.Function<String, Future<?>>() {
            @Override
            public Future<?> apply(String targetTopic) {
                return listenerExecutor.submit(new Runnable() {
                    @Override
                    public void run() {
                        consumeLoop(targetTopic);
                    }
                });
            }
        });
    }

    @Override
    public void destroy() {
        running.set(false);
        for (Future<?> topicListener : topicListeners.values()) {
            topicListener.cancel(true);
        }
        listenerExecutor.shutdownNow();
    }

    void handleRecord(ConsumerRecord<String, String> consumerRecord) {
        MessageHandler handler = messageHandlerRegistry.get(consumerRecord.topic());
        if (handler == null) {
            LOGGER.warn("No Kafka handler registered, topic={}", consumerRecord.topic());
            return;
        }
        handler.handle(toEnvelope(consumerRecord));
    }

    private void consumeLoop(String topic) {
        Consumer<String, String> consumer = kafkaClientConsumerFactory.create(messagingProperties, topic);
        try {
            consumer.subscribe(Collections.singletonList(topic));
            while (running.get() && !Thread.currentThread().isInterrupted()) {
                ConsumerRecords<String, String> records = consumer.poll(currentPollTimeout());
                for (ConsumerRecord<String, String> record : records) {
                    try {
                        handleRecord(record);
                        consumer.commitSync(Collections.singletonMap(
                            new TopicPartition(record.topic(), record.partition()),
                            new OffsetAndMetadata(record.offset() + 1L)
                        ));
                    } catch (RuntimeException ex) {
                        LOGGER.error("Kafka message dispatch failed, topic={}, partition={}, offset={}, reason={}",
                            record.topic(), record.partition(), record.offset(), ex.getMessage());
                    }
                }
            }
        } catch (Exception ex) {
            if (running.get()) {
                LOGGER.error("Kafka consumer loop stopped unexpectedly, topic={}, reason={}", topic, ex.getMessage(), ex);
            }
        } finally {
            consumer.close(CLOSE_TIMEOUT);
        }
    }

    private MessageEnvelope toEnvelope(ConsumerRecord<String, String> consumerRecord) {
        Map<String, String> headers = new HashMap<String, String>();
        for (Header header : consumerRecord.headers()) {
            headers.put(header.key(), new String(header.value(), StandardCharsets.UTF_8));
        }
        return new MessageEnvelope(
            consumerRecord.topic(),
            consumerRecord.key(),
            consumerRecord.value(),
            headers
        );
    }

    private Duration currentPollTimeout() {
        return Duration.ofMillis(messagingProperties.getKafka().getConsumer().getPollTimeoutMs());
    }

    interface KafkaClientConsumerFactory {

        Consumer<String, String> create(MessagingProperties messagingProperties, String topic);
    }

    static final class DefaultKafkaClientConsumerFactory implements KafkaClientConsumerFactory {

        @Override
        public Consumer<String, String> create(MessagingProperties messagingProperties, String topic) {
            Properties properties = new Properties();
            properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, messagingProperties.getKafka().getBootstrapServers());
            properties.put(ConsumerConfig.GROUP_ID_CONFIG, messagingProperties.getKafka().getConsumer().getGroupId());
            properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG,
                messagingProperties.getKafka().getConsumer().getAutoOffsetReset());
            properties.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, Boolean.FALSE.toString());
            properties.put(ConsumerConfig.CLIENT_ID_CONFIG,
                messagingProperties.getKafka().getConsumer().getClientId() + "-" + topic);
            properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
            properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
            KafkaClientPropertySupport.applyCommonKafkaProperties(properties, messagingProperties.getKafka());
            properties.putAll(messagingProperties.getKafka().getConsumer().getProperties());
            return new KafkaConsumer<String, String>(properties);
        }
    }
}
