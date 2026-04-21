package com.company.governance.domain.messaging.entity;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class MessageEnvelope {

    private final String topic;
    private final String key;
    private final String message;
    private final Map<String, String> headers;

    public MessageEnvelope(String topic, String key, String message, Map<String, String> headers) {
        this.topic = topic;
        this.key = key;
        this.message = message;
        this.headers = Collections.unmodifiableMap(new HashMap<String, String>(
            headers == null ? Collections.<String, String>emptyMap() : headers
        ));
    }

    public String getTopic() {
        return topic;
    }

    public String getKey() {
        return key;
    }

    public String getMessage() {
        return message;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }
}
