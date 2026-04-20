package com.company.governance.infrastructure.messaging;

import com.company.governance.domain.messaging.MessageHandler;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MessageHandlerRegistry {

    private final Map<String, MessageHandler> handlers = new ConcurrentHashMap<String, MessageHandler>();

    public void register(String topic, MessageHandler handler) {
        handlers.put(topic, handler);
    }

    public MessageHandler get(String topic) {
        return handlers.get(topic);
    }
}
