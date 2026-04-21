package com.company.governance.infrastructure.messaging;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.company.governance.config.MessagingProperties;
import com.company.sqlforge.common.config.MessagingMode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

class DatabaseMessagePollingJobTest {

    @Test
    void shouldDispatchGovernanceTopicsInDatabaseMode() {
        MessagingProperties messagingProperties = new MessagingProperties();
        messagingProperties.setMode(MessagingMode.DATABASE);
        DatabaseMessageConsumer databaseMessageConsumer = mock(DatabaseMessageConsumer.class);
        @SuppressWarnings("unchecked")
        ObjectProvider<com.company.governance.domain.messaging.MessageConsumer> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(databaseMessageConsumer);

        DatabaseMessagePollingJob job = new DatabaseMessagePollingJob(messagingProperties, provider);
        job.pollConfiguredTopics();

        verify(databaseMessageConsumer).dispatch(GovernanceMessagingTopics.AUDIT_EVENT, 10);
        verify(databaseMessageConsumer).dispatch(GovernanceMessagingTopics.CONFIG_CHANGED, 10);
    }

    @Test
    void shouldSkipPollingOutsideDatabaseMode() {
        MessagingProperties messagingProperties = new MessagingProperties();
        messagingProperties.setMode(MessagingMode.KAFKA);
        DatabaseMessageConsumer databaseMessageConsumer = mock(DatabaseMessageConsumer.class);
        @SuppressWarnings("unchecked")
        ObjectProvider<com.company.governance.domain.messaging.MessageConsumer> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(databaseMessageConsumer);

        DatabaseMessagePollingJob job = new DatabaseMessagePollingJob(messagingProperties, provider);
        job.pollConfiguredTopics();

        verifyNoInteractions(databaseMessageConsumer);
    }
}
