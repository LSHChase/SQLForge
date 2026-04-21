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
import org.junit.jupiter.api.Test;

class MessageAdminApplicationServiceTest {

    @Test
    void shouldReadStatsInDatabaseMode() {
        MessagingProperties messagingProperties = new MessagingProperties();
        messagingProperties.setMode(MessagingMode.DATABASE);
        messagingProperties.getDatabase().setEnabled(true);
        MessageQueueRepository repository = mock(MessageQueueRepository.class);
        MessageQueueRecord stats = new MessageQueueRecord();
        stats.setTotalCount(10L);
        stats.setPendingCount(2L);
        stats.setSentCount(3L);
        stats.setConsumedCount(4L);
        stats.setFailedCount(1L);
        when(repository.fetchQueueStats()).thenReturn(stats);

        MessageAdminApplicationService service = new MessageAdminApplicationService(messagingProperties, repository);
        MessageStatsVO response = service.getMessageStats();

        assertEquals(10L, response.getTotal());
        assertEquals(1L, response.getFailed());
        verify(repository).fetchQueueStats();
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
