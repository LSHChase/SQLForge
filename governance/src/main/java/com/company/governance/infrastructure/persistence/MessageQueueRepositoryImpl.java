package com.company.governance.infrastructure.persistence;

import com.company.governance.domain.message.entity.MessageQueueRecord;
import com.company.governance.domain.message.repository.MessageQueueRepository;
import com.company.governance.infrastructure.persistence.mapper.MessageQueueMapper;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
public class MessageQueueRepositoryImpl implements MessageQueueRepository {

    private final MessageQueueMapper messageQueueMapper;

    public MessageQueueRepositoryImpl(MessageQueueMapper messageQueueMapper) {
        this.messageQueueMapper = messageQueueMapper;
    }

    @Override
    public Long enqueueMessage(MessageQueueRecord messageQueueRecord) {
        messageQueueMapper.insert(messageQueueRecord);
        return messageQueueRecord.getId();
    }

    @Override
    public List<MessageQueueRecord> pollPendingMessages(String topic, int batchSize) {
        return messageQueueMapper.selectPendingMessages(topic, batchSize);
    }

    @Override
    public int markMessageSent(Long id) {
        return messageQueueMapper.updateStatus(id, "SENT", null, null);
    }

    @Override
    public int markMessageConsumed(Long id) {
        return messageQueueMapper.updateStatus(id, "CONSUMED", null, "CURRENT_TIMESTAMP");
    }

    @Override
    public int markMessageFailed(Long id, String errorLog) {
        return messageQueueMapper.updateStatus(id, "FAILED", errorLog, null);
    }

    @Override
    public int retryFailedMessages() {
        return messageQueueMapper.retryFailedMessages();
    }

    @Override
    public MessageQueueRecord fetchQueueStats() {
        return messageQueueMapper.selectQueueStats();
    }
}
