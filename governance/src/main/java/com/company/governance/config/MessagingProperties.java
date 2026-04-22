package com.company.governance.config;

import com.company.sqlforge.common.config.MessagingMode;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "messaging")
public class MessagingProperties {

    private MessagingMode mode = MessagingMode.DATABASE;
    private final KafkaProperties kafka = new KafkaProperties();
    private final DatabaseProperties database = new DatabaseProperties();

    public MessagingMode getMode() {
        return mode;
    }

    public void setMode(MessagingMode mode) {
        this.mode = mode;
    }

    public KafkaProperties getKafka() {
        return kafka;
    }

    public DatabaseProperties getDatabase() {
        return database;
    }

    public static class KafkaProperties {

        private boolean enabled;
        private String bootstrapServers;
        private String securityProtocol = "PLAINTEXT";
        private String saslMechanism;
        private String saslJaasConfig;
        private String sslTruststoreLocation;
        private String sslTruststorePassword;
        private final ProducerProperties producer = new ProducerProperties();
        private final ConsumerProperties consumer = new ConsumerProperties();
        private final Map<String, String> properties = new LinkedHashMap<String, String>();

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getBootstrapServers() {
            return bootstrapServers;
        }

        public void setBootstrapServers(String bootstrapServers) {
            this.bootstrapServers = bootstrapServers;
        }

        public String getSecurityProtocol() {
            return securityProtocol;
        }

        public void setSecurityProtocol(String securityProtocol) {
            this.securityProtocol = securityProtocol;
        }

        public String getSaslMechanism() {
            return saslMechanism;
        }

        public void setSaslMechanism(String saslMechanism) {
            this.saslMechanism = saslMechanism;
        }

        public String getSaslJaasConfig() {
            return saslJaasConfig;
        }

        public void setSaslJaasConfig(String saslJaasConfig) {
            this.saslJaasConfig = saslJaasConfig;
        }

        public String getSslTruststoreLocation() {
            return sslTruststoreLocation;
        }

        public void setSslTruststoreLocation(String sslTruststoreLocation) {
            this.sslTruststoreLocation = sslTruststoreLocation;
        }

        public String getSslTruststorePassword() {
            return sslTruststorePassword;
        }

        public void setSslTruststorePassword(String sslTruststorePassword) {
            this.sslTruststorePassword = sslTruststorePassword;
        }

        public ProducerProperties getProducer() {
            return producer;
        }

        public ConsumerProperties getConsumer() {
            return consumer;
        }

        public Map<String, String> getProperties() {
            return properties;
        }
    }

    public static class ProducerProperties {

        private String acks = "all";
        private int retries = 3;
        private String clientId = "governance-producer";
        private int requestTimeoutMs = 15000;
        private int deliveryTimeoutMs = 30000;
        private int maxBlockMs = 15000;
        private final Map<String, String> properties = new LinkedHashMap<String, String>();

        public String getAcks() {
            return acks;
        }

        public void setAcks(String acks) {
            this.acks = acks;
        }

        public int getRetries() {
            return retries;
        }

        public void setRetries(int retries) {
            this.retries = retries;
        }

        public String getClientId() {
            return clientId;
        }

        public void setClientId(String clientId) {
            this.clientId = clientId;
        }

        public int getRequestTimeoutMs() {
            return requestTimeoutMs;
        }

        public void setRequestTimeoutMs(int requestTimeoutMs) {
            this.requestTimeoutMs = requestTimeoutMs;
        }

        public int getDeliveryTimeoutMs() {
            return deliveryTimeoutMs;
        }

        public void setDeliveryTimeoutMs(int deliveryTimeoutMs) {
            this.deliveryTimeoutMs = deliveryTimeoutMs;
        }

        public int getMaxBlockMs() {
            return maxBlockMs;
        }

        public void setMaxBlockMs(int maxBlockMs) {
            this.maxBlockMs = maxBlockMs;
        }

        public Map<String, String> getProperties() {
            return properties;
        }
    }

    public static class ConsumerProperties {

        private String groupId = "sqlforge-group";
        private String autoOffsetReset = "earliest";
        private String clientId = "governance-consumer";
        private int pollTimeoutMs = 1000;
        private final Map<String, String> properties = new LinkedHashMap<String, String>();

        public String getGroupId() {
            return groupId;
        }

        public void setGroupId(String groupId) {
            this.groupId = groupId;
        }

        public String getAutoOffsetReset() {
            return autoOffsetReset;
        }

        public void setAutoOffsetReset(String autoOffsetReset) {
            this.autoOffsetReset = autoOffsetReset;
        }

        public String getClientId() {
            return clientId;
        }

        public void setClientId(String clientId) {
            this.clientId = clientId;
        }

        public int getPollTimeoutMs() {
            return pollTimeoutMs;
        }

        public void setPollTimeoutMs(int pollTimeoutMs) {
            this.pollTimeoutMs = pollTimeoutMs;
        }

        public Map<String, String> getProperties() {
            return properties;
        }
    }

    public static class DatabaseProperties {

        private boolean enabled;
        private int pollInterval;
        private int maxRetry;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public int getPollInterval() {
            return pollInterval;
        }

        public void setPollInterval(int pollInterval) {
            this.pollInterval = pollInterval;
        }

        public int getMaxRetry() {
            return maxRetry;
        }

        public void setMaxRetry(int maxRetry) {
            this.maxRetry = maxRetry;
        }
    }
}
