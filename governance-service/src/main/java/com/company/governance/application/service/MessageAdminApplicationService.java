package com.company.governance.application.service;

import com.company.governance.application.controller.vo.MessageRetryResultVO;
import com.company.governance.application.controller.vo.MessageStatsVO;
import com.company.governance.domain.message.entity.MessageQueueRecord;
import com.company.governance.domain.message.repository.MessageQueueRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class MessageAdminApplicationService {

    private static final Logger log = LoggerFactory.getLogger(MessageAdminApplicationService.class);

    private final MessageQueueRepository messageQueueRepository;

    public MessageAdminApplicationService(MessageQueueRepository messageQueueRepository) {
        this.messageQueueRepository = messageQueueRepository;
    }

    public MessageRetryResultVO retryFailedMessages() {
        int retriedCount = messageQueueRepository.retryFailedMessages();
        log.info("Retried failed database-queue messages, retriedCount={}", retriedCount);
        return new MessageRetryResultVO(retriedCount, "ACCEPTED");
    }

    public MessageStatsVO getMessageStats() {
        MessageQueueRecord stats = messageQueueRepository.fetchQueueStats();
        return new MessageStatsVO(
            defaultLong(stats.getTotalCount()),
            defaultLong(stats.getPendingCount()),
            defaultLong(stats.getSentCount()),
            defaultLong(stats.getConsumedCount()),
            defaultLong(stats.getFailedCount())
        );
    }

    private long defaultLong(Long value) {
        return value == null ? 0L : value;
    }
}
