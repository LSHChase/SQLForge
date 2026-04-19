package com.company.governance.infrastructure.persistence.mapper;

import com.company.governance.domain.message.entity.MessageQueueRecord;

public interface MessageQueueMapper {

    int retryFailedMessages();

    MessageQueueRecord selectQueueStats();
}
