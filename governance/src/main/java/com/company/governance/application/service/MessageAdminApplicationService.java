package com.company.governance.application.service;

import com.company.governance.application.controller.vo.MessageRetryResultVO;
import com.company.governance.application.controller.vo.MessageStatsVO;
import com.company.governance.config.MessagingProperties;
import com.company.governance.domain.message.entity.MessageQueueRecord;
import com.company.governance.domain.message.repository.MessageQueueRepository;
import com.company.sqlforge.common.config.MessagingMode;
import com.company.sqlforge.common.constants.ErrorCodeConstants;
import com.company.sqlforge.common.exception.BizException;
import com.company.sqlforge.common.log.OperationLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class MessageAdminApplicationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(MessageAdminApplicationService.class);

    private final MessagingProperties messagingProperties;
    private final MessageQueueRepository messageQueueRepository;
    private final GovernanceMetricsRecorder metricsRecorder;

    @Autowired
    public MessageAdminApplicationService(MessagingProperties messagingProperties,
                                          MessageQueueRepository messageQueueRepository,
                                          GovernanceMetricsRecorder metricsRecorder) {
        this.messagingProperties = messagingProperties;
        this.messageQueueRepository = messageQueueRepository;
        this.metricsRecorder = metricsRecorder;
    }

    MessageAdminApplicationService(MessagingProperties messagingProperties,
                                   MessageQueueRepository messageQueueRepository) {
        this(messagingProperties, messageQueueRepository, GovernanceMetricsRecorder.noop());
    }

    @OperationLog(operation = "GOVERNANCE_MESSAGE_RETRY", entity = "kafka_message_queue")
    public MessageRetryResultVO retryFailedMessages() {
        ensureDatabaseQueueAdminEnabled(
            ErrorCodeConstants.GOVERNANCE_MESSAGE_RETRY_FAILED,
            "消息重试仅在 DATABASE 消息模式下支持"
        );
        int retriedCount = messageQueueRepository.retryFailedMessages();
        metricsRecorder.recordRetriedMessages(retriedCount);
        LOGGER.info("已重试失败数据库队列消息，retriedCount={}", retriedCount);
        return new MessageRetryResultVO(retriedCount, "ACCEPTED");
    }

    @OperationLog(operation = "GOVERNANCE_MESSAGE_STATS", entity = "kafka_message_queue")
    public MessageStatsVO getMessageStats() {
        ensureDatabaseQueueAdminEnabled(
            ErrorCodeConstants.GOVERNANCE_MESSAGE_STATS_UNAVAILABLE,
            "消息统计仅在 DATABASE 消息模式下可用"
        );
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

    private void ensureDatabaseQueueAdminEnabled(int errorCode, String message) {
        if (messagingProperties.getMode() != MessagingMode.DATABASE
            || !messagingProperties.getDatabase().isEnabled()) {
            throw new BizException(errorCode, HttpStatus.CONFLICT, message);
        }
    }
}
