package com.cts.notificationservice.config;

import com.cts.notificationservice.dto.NotificationEventMessage;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.*;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.util.backoff.ExponentialBackOff;

import java.util.HashMap;
import java.util.Map;

@EnableKafka
@Configuration
public class KafkaConfig {

    @Value("${spring.kafka.bootstrap-servers:localhost:9092}")
    private String bootstrapServers;

    @Value("${spring.kafka.consumer.group-id:notification-service-group}")
    private String groupId;

    // ── Consumer ──────────────────────────────────────────────────────────────

    @Bean
    public ConsumerFactory<String, NotificationEventMessage> consumerFactory() {
        JsonDeserializer<NotificationEventMessage> deser =
                new JsonDeserializer<>(NotificationEventMessage.class, false);
        deser.addTrustedPackages("com.cts.*");

        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 50);

        return new DefaultKafkaConsumerFactory<>(props, new StringDeserializer(), deser);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, NotificationEventMessage>
    kafkaListenerContainerFactory(
            ConsumerFactory<String, NotificationEventMessage> consumerFactory,
            KafkaTemplate<String, NotificationEventMessage> dltKafkaTemplate) {

        // Exponential backoff: 1s → 2s → 4s, then DLT (3 total Kafka-level retries)
        ExponentialBackOff backOff = new ExponentialBackOff(1000L, 2.0);
        backOff.setMaxAttempts(3);

        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(dltKafkaTemplate);
        DefaultErrorHandler errorHandler = new DefaultErrorHandler(recoverer, backOff);

        ConcurrentKafkaListenerContainerFactory<String, NotificationEventMessage> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        factory.setCommonErrorHandler(errorHandler);
        factory.getContainerProperties().setAckMode(
                org.springframework.kafka.listener.ContainerProperties.AckMode.RECORD);
        return factory;
    }

    // ── Producer (DLT publisher only) ─────────────────────────────────────────

    @Bean
    public ProducerFactory<String, NotificationEventMessage> producerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        props.put(JsonSerializer.ADD_TYPE_INFO_HEADERS, false);
        return new DefaultKafkaProducerFactory<>(props);
    }

    @Bean
    public KafkaTemplate<String, NotificationEventMessage> dltKafkaTemplate(
            ProducerFactory<String, NotificationEventMessage> producerFactory) {
        return new KafkaTemplate<>(producerFactory);
    }
}
