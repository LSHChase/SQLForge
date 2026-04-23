package com.company.governance.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.company.governance.application.controller.vo.MessageStatsVO;
import com.company.governance.config.MessagingProperties;
import com.company.governance.domain.message.entity.MessageQueueRecord;
import com.company.governance.domain.message.repository.MessageQueueRepository;
import com.company.sqlforge.common.config.MessagingMode;
import com.company.sqlforge.common.constants.ErrorCodeConstants;
import com.company.sqlforge.common.exception.BizException;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

class MessageAdminApplicationServiceTest {

    @Test
    void shouldReadStatsInDatabaseMode() {
        MessagingProperties messagingProperties = new MessagingProperties();
        messagingProperties.setMode(MessagingMode.DATABASE);
        messagingProperties.getDatabase().setEnabled(true);
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        MessageQueueRepository repository = mock(MessageQueueRepository.class);
        MessageQueueRecord stats = new MessageQueueRecord();
        stats.setTotalCount(10L);
        stats.setPendingCount(2L);
        stats.setSentCount(3L);
        stats.setConsumedCount(4L);
        stats.setFailedCount(1L);
        when(repository.fetchQueueStats()).thenReturn(stats);

        GovernanceMetricsRecorder metricsRecorder =
            new GovernanceMetricsRecorder(meterRegistry, repository, messagingProperties);
        MessageAdminApplicationService service =
            new MessageAdminApplicationService(messagingProperties, repository, metricsRecorder);
        MessageStatsVO response = service.getMessageStats();

        assertEquals(10L, response.getTotal());
        assertEquals(1L, response.getFailed());
        verify(repository).fetchQueueStats();
        assertEquals(10.0D, meterRegistry.get("sqlforge.governance.message.queue.total").gauge().value());
        assertEquals(2.0D, meterRegistry.get("sqlforge.governance.message.queue.pending").gauge().value());
        assertEquals(1.0D, meterRegistry.get("sqlforge.governance.message.queue.failed").gauge().value());
    }

    @Test
    void shouldCountRetriedMessagesInDatabaseMode() {
        MessagingProperties messagingProperties = new MessagingProperties();
        messagingProperties.setMode(MessagingMode.DATABASE);
        messagingProperties.getDatabase().setEnabled(true);
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        MessageQueueRepository repository = mock(MessageQueueRepository.class);
        when(repository.retryFailedMessages()).thenReturn(3);

        GovernanceMetricsRecorder metricsRecorder =
            new GovernanceMetricsRecorder(meterRegistry, repository, messagingProperties);
        MessageAdminApplicationService service =
            new MessageAdminApplicationService(messagingProperties, repository, metricsRecorder);

        service.retryFailedMessages();

        assertEquals(3.0D, meterRegistry.get("sqlforge.governance.message.retry.messages").tags(
            "messaging_mode", "DATABASE"
        ).counter().count());
    }

    @Test
    void shouldRejectRetryOutsideDatabaseMode() {
        MessagingProperties messagingProperties = new MessagingProperties();
        messagingProperties.setMode(MessagingMode.KAFKA);
        messagingProperties.getDatabase().setEnabled(false);
        MessageQueueRepository repository = mock(MessageQueueRepository.class);
        MessageAdminApplicationService service = new MessageAdminApplicationService(messagingProperties, repository);

        BizException ex = assertThrows(BizException.class, service::retryFailedMessages);

        assertEquals(ErrorCodeConstants.GOVERNANCE_MESSAGE_RETRY_FAILED, ex.getCode());
    }
}
