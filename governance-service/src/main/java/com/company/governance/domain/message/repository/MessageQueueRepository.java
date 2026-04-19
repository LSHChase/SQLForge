package com.company.governance.domain.message.repository;

import com.company.governance.domain.message.entity.MessageQueueRecord;

public interface MessageQueueRepository {

    int retryFailedMessages();

    MessageQueueRecord fetchQueueStats();
}
