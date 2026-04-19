package com.company.governance.infrastructure.persistence;

import com.company.governance.domain.message.entity.MessageQueueRecord;
import com.company.governance.domain.message.repository.MessageQueueRepository;
import com.company.governance.infrastructure.persistence.mapper.MessageQueueMapper;
import org.springframework.stereotype.Repository;

@Repository
public class MessageQueueRepositoryImpl implements MessageQueueRepository {

    private final MessageQueueMapper messageQueueMapper;

    public MessageQueueRepositoryImpl(MessageQueueMapper messageQueueMapper) {
        this.messageQueueMapper = messageQueueMapper;
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
