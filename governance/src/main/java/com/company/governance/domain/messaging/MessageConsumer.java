package com.company.governance.domain.messaging;

import com.company.governance.domain.messaging.entity.MessageEnvelope;
import java.util.List;

public interface MessageConsumer {

    List<MessageEnvelope> poll(String topic, int batchSize);

    void listen(String topic, MessageHandler handler);
}
