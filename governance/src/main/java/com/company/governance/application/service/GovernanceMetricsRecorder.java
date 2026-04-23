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
        registerQueueGauge(METRIC_QUEUE_TOTAL, "Current governance message-queue total size.", QueueCountType.TOTAL);
        registerQueueGauge(METRIC_QUEUE_PENDING, "Current governance message-queue pending size.", QueueCountType.PENDING);
        registerQueueGauge(METRIC_QUEUE_FAILED, "Current governance message-queue failed size.", QueueCountType.FAILED);
    }

    static GovernanceMetricsRecorder noop() {
        MessagingProperties messagingProperties = new MessagingProperties();
        messagingProperties.setMode(MessagingMode.MOCK);
        return new GovernanceMetricsRecorder(new SimpleMeterRegistry(), null, messagingProperties);
    }

    public void recordAuditFallback() {
        Counter.builder(METRIC_AUDIT_FALLBACKS)
            .description("Governance audit events that fell back to the database queue.")
            .tags("messaging_mode", messagingMode())
            .register(meterRegistry)
            .increment();
    }

    public void recordRetriedMessages(int retriedCount) {
        if (retriedCount <= 0) {
            return;
        }
        Counter.builder(METRIC_MESSAGE_RETRY_MESSAGES)
            .description("Governance database-queue messages retried by operators.")
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
            LOGGER.warn("Failed to sample governance message queue gauge, type={}, reason={}",
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
