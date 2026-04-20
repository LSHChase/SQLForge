package com.company.governance.infrastructure.messaging;

import com.company.governance.domain.message.entity.MessageQueueRecord;
import com.company.governance.domain.message.repository.MessageQueueRepository;
import com.company.governance.domain.messaging.MessageConsumer;
import com.company.governance.domain.messaging.MessageHandler;
import com.company.governance.domain.messaging.entity.MessageEnvelope;
import com.company.sqlforge.common.constants.ErrorCodeConstants;
import com.company.sqlforge.common.exception.BizException;
import com.company.sqlforge.common.utils.JsonUtils;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;

public class DatabaseMessageConsumer implements MessageConsumer {

    private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseMessageConsumer.class);

    private final MessageQueueRepository messageQueueRepository;
    private final MessageHandlerRegistry messageHandlerRegistry;

    public DatabaseMessageConsumer(MessageQueueRepository messageQueueRepository,
                                   MessageHandlerRegistry messageHandlerRegistry) {
        this.messageQueueRepository = messageQueueRepository;
        this.messageHandlerRegistry = messageHandlerRegistry;
    }

    @Override
    public List<MessageEnvelope> poll(String topic, int batchSize) {
        List<MessageQueueRecord> records = messageQueueRepository.pollPendingMessages(topic, batchSize);
        List<MessageEnvelope> envelopes = new ArrayList<MessageEnvelope>(records.size());
        for (MessageQueueRecord record : records) {
            envelopes.add(toEnvelope(record));
            messageQueueRepository.markMessageSent(record.getId());
        }
        return envelopes;
    }

    @Override
    public void listen(String topic, MessageHandler handler) {
        messageHandlerRegistry.register(topic, handler);
    }

    public int dispatch(String topic, int batchSize) {
        MessageHandler handler = messageHandlerRegistry.get(topic);
        if (handler == null) {
            LOGGER.debug("No message handler registered, topic={}", topic);
            return 0;
        }
        List<MessageQueueRecord> records = messageQueueRepository.pollPendingMessages(topic, batchSize);
        int consumed = 0;
        for (MessageQueueRecord record : records) {
            try {
                handler.handle(toEnvelope(record));
                messageQueueRepository.markMessageConsumed(record.getId());
                consumed++;
            } catch (RuntimeException ex) {
                messageQueueRepository.markMessageFailed(record.getId(), ex.getMessage());
                LOGGER.error("Database message dispatch failed, topic={}, id={}, reason={}",
                    topic, record.getId(), ex.getMessage());
            }
        }
        return consumed;
    }

    private MessageEnvelope toEnvelope(MessageQueueRecord record) {
        Map<String, String> headers = parseHeaders(record.getHeaders());
        if (record.getTopic() == null || record.getMessageBody() == null) {
            throw new BizException(
                ErrorCodeConstants.SYSTEM_AUDIT_CONTRACT_INVALID,
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Stored message is missing required fields"
            );
        }
        return new MessageEnvelope(record.getTopic(), record.getPartitionKey(), record.getMessageBody(), headers);
    }

    @SuppressWarnings("unchecked")
    private Map<String, String> parseHeaders(String json) {
        if (json == null || json.trim().isEmpty()) {
            return Collections.emptyMap();
        }
        return JsonUtils.fromJson(json, Map.class);
    }
}
