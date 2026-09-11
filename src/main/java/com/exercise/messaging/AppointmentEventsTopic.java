package com.exercise.messaging;

import com.exercise.config.KafkaTopicProperties;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.config.TopicConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/** Created here because automatic topic creation is disabled on the broker. */
@Configuration
public class AppointmentEventsTopic {

    public static final String EVENT_ID_HEADER = "event_id";
    public static final String EVENT_TYPE_HEADER = "event_type";

    @Bean
    NewTopic appointmentEvents(KafkaTopicProperties properties) {
        return TopicBuilder.name(properties.name())
                .partitions(properties.partitions())
                .replicas(1)
                .config(TopicConfig.RETENTION_MS_CONFIG, String.valueOf(properties.retention().toMillis()))
                .build();
    }
}
