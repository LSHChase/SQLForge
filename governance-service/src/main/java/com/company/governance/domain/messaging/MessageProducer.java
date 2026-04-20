package com.company.governance.domain.messaging;

import java.util.Map;

public interface MessageProducer {

    void send(String topic, String key, String message, Map<String, String> headers);
}
