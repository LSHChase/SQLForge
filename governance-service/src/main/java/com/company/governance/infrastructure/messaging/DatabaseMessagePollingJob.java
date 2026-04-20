package com.company.governance.infrastructure.messaging;

import com.company.governance.config.MessagingProperties;
import com.company.sqlforge.common.config.MessagingMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class DatabaseMessagePollingJob {

    private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseMessagePollingJob.class);

    private final MessagingProperties messagingProperties;
    private final DatabaseMessageConsumer databaseMessageConsumer;

    public DatabaseMessagePollingJob(MessagingProperties messagingProperties,
                                     org.springframework.beans.factory.ObjectProvider<com.company.governance.domain.messaging.MessageConsumer> messageConsumerProvider) {
        this.messagingProperties = messagingProperties;
        com.company.governance.domain.messaging.MessageConsumer messageConsumer = messageConsumerProvider.getIfAvailable();
        this.databaseMessageConsumer = messageConsumer instanceof DatabaseMessageConsumer
            ? (DatabaseMessageConsumer) messageConsumer
            : null;
    }

    @Scheduled(fixedDelayString = "${messaging.database.poll-interval:5000}")
    public void pollConfiguredTopics() {
        if (messagingProperties.getMode() != MessagingMode.DATABASE || databaseMessageConsumer == null) {
            return;
        }
        int consumedAudit = databaseMessageConsumer.dispatch(
            GovernanceMessagingTopics.AUDIT_EVENT,
            10
        );
        int consumedConfig = databaseMessageConsumer.dispatch(
            GovernanceMessagingTopics.CONFIG_CHANGED,
            10
        );
        if (consumedAudit > 0 || consumedConfig > 0) {
            LOGGER.info("Database message polling completed, auditConsumed={}, configConsumed={}",
                consumedAudit, consumedConfig);
        }
    }
}
