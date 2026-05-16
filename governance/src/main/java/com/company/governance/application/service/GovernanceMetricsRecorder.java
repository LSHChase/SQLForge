package com.company.governance.application.service;

import com.company.governance.config.MessagingProperties;
import com.company.governance.domain.message.entity.MessageQueueRecord;
import com.company.governance.domain.message.repository.MessageQueueRepository;
import com.company.sqlforge.common.config.MessagingMode;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class GovernanceMetricsRecorder {

    private static final Logger LOGGER = LoggerFactory.getLogger(GovernanceMetricsRecorder.class);

    private static final String METRIC_AUDIT_FALLBACKS = "sqlforge.governance.audit.fallbacks";
    private static final String METRIC_MESSAGE_RETRY_MESSAGES = "sqlforge.governance.message.retry.messages";
    private static final String METRIC_QUEUE_TOTAL = "sqlforge.governance.message.queue.total";
    private static final String METRIC_QUEUE_PENDING = "sqlforge.governance.message.queue.pending";
    private static final String METRIC_QUEUE_FAILED = "sqlforge.governance.message.queue.failed";
    private static final String UNKNOWN_VALUE = "UNKNOWN";

    private final MeterRegistry meterRegistry;
    private final MessageQueueRepository messageQueueRepository;
    private final MessagingProperties messagingProperties;

    public GovernanceMetricsRecorder(MeterRegistry meterRegistry,
                                     MessageQueueRepository messageQueueRepository,
                                     MessagingProperties messagingProperties) {
        this.meterRegistry = meterRegistry;
        this.messageQueueRepository = messageQueueRepository;
        this.messagingProperties = messagingProperties;
        registerQueueGauge(METRIC_QUEUE_TOTAL, "当前治理消息队列总量。", QueueCountType.TOTAL);
        registerQueueGauge(METRIC_QUEUE_PENDING, "当前治理消息队列待处理数量。", QueueCountType.PENDING);
        registerQueueGauge(METRIC_QUEUE_FAILED, "当前治理消息队列失败数量。", QueueCountType.FAILED);
    }

    static GovernanceMetricsRecorder noop() {
        MessagingProperties messagingProperties = new MessagingProperties();
        messagingProperties.setMode(MessagingMode.MOCK);
        return new GovernanceMetricsRecorder(new SimpleMeterRegistry(), null, messagingProperties);
    }

    public void recordAuditFallback() {
        Counter.builder(METRIC_AUDIT_FALLBACKS)
            .description("降级写入数据库队列的治理审计事件数。")
            .tags("messaging_mode", messagingMode())
            .register(meterRegistry)
            .increment();
    }

    public void recordRetriedMessages(int retriedCount) {
        if (retriedCount <= 0) {
            return;
        }
        Counter.builder(METRIC_MESSAGE_RETRY_MESSAGES)
            .description("操作员重试的治理数据库队列消息数。")
            .tags("messaging_mode", messagingMode())
            .register(meterRegistry)
            .increment(retriedCount);
    }

    private void registerQueueGauge(String metricName, String description, QueueCountType queueCountType) {
        Gauge.builder(metricName, this, value -> value.queueGaugeValue(queueCountType))
            .description(description)
            .tag("messaging_mode", messagingMode())
            .register(meterRegistry);
    }

    private double queueGaugeValue(QueueCountType queueCountType) {
        if (!isDatabaseQueueEnabled() || messageQueueRepository == null) {
            return 0D;
        }
        try {
            MessageQueueRecord stats = messageQueueRepository.fetchQueueStats();
            if (stats == null) {
                return 0D;
            }
            switch (queueCountType) {
                case TOTAL:
                    return defaultLong(stats.getTotalCount());
                case PENDING:
                    return defaultLong(stats.getPendingCount());
                case FAILED:
                    return defaultLong(stats.getFailedCount());
                default:
                    return 0D;
            }
        } catch (RuntimeException ex) {
            LOGGER.warn("采样治理消息队列 gauge 失败，type={}, reason={}",
                queueCountType.name(), ex.getMessage());
            return 0D;
        }
    }

    private boolean isDatabaseQueueEnabled() {
        return messagingProperties != null
            && messagingProperties.getMode() == MessagingMode.DATABASE
            && messagingProperties.getDatabase().isEnabled();
    }

    private String messagingMode() {
        return messagingProperties == null || messagingProperties.getMode() == null
            ? UNKNOWN_VALUE
            : messagingProperties.getMode().name();
    }

    private double defaultLong(Long value) {
        return value == null ? 0D : value.doubleValue();
    }

    private enum QueueCountType {
        TOTAL,
        PENDING,
        FAILED
    }
}
