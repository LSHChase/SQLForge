package com.company.governance.infrastructure.messaging;

import com.company.governance.domain.message.entity.MessageQueueRecord;
import com.company.governance.domain.message.repository.MessageQueueRepository;
import com.company.governance.domain.messaging.MessageProducer;
import com.company.sqlforge.common.utils.JsonUtils;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DatabaseMessageProducer implements MessageProducer {

    private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseMessageProducer.class);

    private final MessageQueueRepository messageQueueRepository;

    public DatabaseMessageProducer(MessageQueueRepository messageQueueRepository) {
        this.messageQueueRepository = messageQueueRepository;
    }

    @Override
    public void send(String topic, String key, String message, Map<String, String> headers) {
        MessageQueueRecord messageQueueRecord = MessageQueueRecord.pending(
            topic,
            key,
            message,
            JsonUtils.toJson(headers)
        );
        Long id = messageQueueRepository.enqueueMessage(messageQueueRecord);
        LOGGER.info("数据库消息已入队，id={}, topic={}, partitionKey={}", id, topic, key);
    }
}
