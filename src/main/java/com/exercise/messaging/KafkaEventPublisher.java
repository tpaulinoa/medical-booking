package com.exercise.messaging;

import com.exercise.config.KafkaTopicProperties;
import com.exercise.outbox.EventPublisher;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
class KafkaEventPublisher implements EventPublisher {

    private final KafkaTemplate<String, String> kafka;
    private final KafkaTopicProperties topic;

    KafkaEventPublisher(KafkaTemplate<String, String> kafka, KafkaTopicProperties topic) {
        this.kafka = kafka;
        this.topic = topic;
    }

    @Override
    public void publish(UUID eventId, String domainKey, String eventType, String payload) {
        ProducerRecord<String, String> record = new ProducerRecord<>(topic.name(), domainKey, payload);
        record.headers()
                .add(AppointmentEventsTopic.EVENT_ID_HEADER, eventId.toString().getBytes(StandardCharsets.UTF_8))
                .add(AppointmentEventsTopic.EVENT_TYPE_HEADER, eventType.getBytes(StandardCharsets.UTF_8));

        try {
            kafka.send(record).get();
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while publishing " + eventId, interrupted);
        } catch (ExecutionException rejected) {
            throw new IllegalStateException("Broker rejected " + eventId, rejected.getCause());
        }
    }
}
