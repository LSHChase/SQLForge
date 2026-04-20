package com.company.governance.domain.messaging;

import com.company.governance.domain.messaging.entity.MessageEnvelope;

public interface MessageHandler {

    void handle(MessageEnvelope messageEnvelope);
}
