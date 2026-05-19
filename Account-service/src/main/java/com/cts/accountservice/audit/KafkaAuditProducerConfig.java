package com.cts.accountservice.audit;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.scheduling.annotation.EnableAsync;

import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableAsync
public class KafkaAuditProducerConfig {

    @Value("${spring.kafka.bootstrap-servers:localhost:9092}")
    private String bootstrapServers;

    @Bean("auditProducerFactory")
    public ProducerFactory<String, AuditEventMessage> auditProducerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        // Exactly-once semantics for audit events — no duplicates on broker side
        props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        props.put(ProducerConfig.ACKS_CONFIG, "all");
        props.put(ProducerConfig.RETRIES_CONFIG, 3);
        props.put(JsonSerializer.ADD_TYPE_INFO_HEADERS, false);
        return new DefaultKafkaProducerFactory<>(props);
    }

    @Bean("auditKafkaTemplate")
    public KafkaTemplate<String, AuditEventMessage> auditKafkaTemplate(
            @org.springframework.beans.factory.annotation.Qualifier("auditProducerFactory")
            ProducerFactory<String, AuditEventMessage> auditProducerFactory) {
        return new KafkaTemplate<>(auditProducerFactory);
    }

    // ── Notification producer ─────────────────────────────────────────────────

    @Bean("notificationProducerFactory")
    public org.springframework.kafka.core.ProducerFactory<String, NotificationEventMessage>
    notificationProducerFactory() {
        java.util.Map<String, Object> props = new java.util.HashMap<>();
        props.put(org.apache.kafka.clients.producer.ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(org.apache.kafka.clients.producer.ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,
                org.apache.kafka.common.serialization.StringSerializer.class);
        props.put(org.apache.kafka.clients.producer.ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,
                org.springframework.kafka.support.serializer.JsonSerializer.class);
        props.put(org.apache.kafka.clients.producer.ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        props.put(org.apache.kafka.clients.producer.ProducerConfig.ACKS_CONFIG, "all");
        props.put(org.apache.kafka.clients.producer.ProducerConfig.RETRIES_CONFIG, 3);
        props.put(org.springframework.kafka.support.serializer.JsonSerializer.ADD_TYPE_INFO_HEADERS, false);
        return new org.springframework.kafka.core.DefaultKafkaProducerFactory<>(props);
    }

    @Bean("notificationKafkaTemplate")
    public KafkaTemplate<String, NotificationEventMessage> notificationKafkaTemplate(
            @org.springframework.beans.factory.annotation.Qualifier("notificationProducerFactory")
            org.springframework.kafka.core.ProducerFactory<String, NotificationEventMessage> notificationProducerFactory) {
        return new KafkaTemplate<>(notificationProducerFactory);
    }
}
