package com.company.governance.infrastructure.messaging;

import com.company.governance.config.MessagingProperties;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.common.config.SaslConfigs;
import org.apache.kafka.common.config.SslConfigs;
import org.springframework.util.StringUtils;

import java.util.Properties;

final class KafkaClientPropertySupport {

    private KafkaClientPropertySupport() {
    }

    static void applyCommonKafkaProperties(Properties properties, MessagingProperties.KafkaProperties kafkaProperties) {
        putIfHasText(properties, CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, kafkaProperties.getSecurityProtocol());
        putIfHasText(properties, SaslConfigs.SASL_MECHANISM, kafkaProperties.getSaslMechanism());
        putIfHasText(properties, SaslConfigs.SASL_JAAS_CONFIG, kafkaProperties.getSaslJaasConfig());
        putIfHasText(properties, SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG, kafkaProperties.getSslTruststoreLocation());
        putIfHasText(properties, SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG, kafkaProperties.getSslTruststorePassword());
        properties.putAll(kafkaProperties.getProperties());
    }

    private static void putIfHasText(Properties properties, String key, String value) {
        if (StringUtils.hasText(value)) {
            properties.put(key, value);
        }
    }
}
