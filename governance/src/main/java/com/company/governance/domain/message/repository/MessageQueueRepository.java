package com.company.governance.domain.message.repository;

import com.company.governance.domain.message.entity.MessageQueueRecord;
import java.util.List;

public interface MessageQueueRepository {

    Long enqueueMessage(MessageQueueRecord messageQueueRecord);

    List<MessageQueueRecord> pollPendingMessages(String topic, int batchSize);

    int markMessageSent(Long id);

    int markMessageConsumed(Long id);

    int markMessageFailed(Long id, String errorLog);

    int retryFailedMessages();

    MessageQueueRecord fetchQueueStats();
}
