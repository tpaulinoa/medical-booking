package com.exercise.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.function.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Checks, acts, then records the event. Recording after the action means a crash can repeat an action but never lose
 * one, and a repeated call is harmless because it carries the appointment id.
 *
 * <p>Events of the same appointment share a partition, so a consumer group never handles them at the same time.
 */
@Component
public class IdempotentEventHandler {

    private static final Logger log = LoggerFactory.getLogger(IdempotentEventHandler.class);

    private final ProcessedEventRepository processedEvents;
    private final ObjectMapper objectMapper;

    IdempotentEventHandler(ProcessedEventRepository processedEvents, ObjectMapper objectMapper) {
        this.processedEvents = processedEvents;
        this.objectMapper = objectMapper;
    }

    public void handle(
            ConsumerRecord<String, String> record,
            String consumerName,
            Consumer<AppointmentCreatedEvent> action) {

        UUID eventId = eventIdOf(record);

        if (processedEvents.existsById(new ProcessedEvent.ProcessedEventId(eventId, consumerName))) {
            log.info("{} already handled {}", consumerName, eventId);
            return;
        }

        action.accept(deserialise(record));

        if (processedEvents.recordIfAbsent(eventId, consumerName) == 0) {
            log.debug("{} raced on recording {}", consumerName, eventId);
        }
    }

    private UUID eventIdOf(ConsumerRecord<String, String> record) {
        var header = record.headers().lastHeader(AppointmentEventsTopic.EVENT_ID_HEADER);
        if (header == null) {
            throw new IllegalStateException("event on " + record.topic() + " carries no "
                    + AppointmentEventsTopic.EVENT_ID_HEADER + " header");
        }
        return UUID.fromString(new String(header.value(), StandardCharsets.UTF_8));
    }

    private AppointmentCreatedEvent deserialise(ConsumerRecord<String, String> record) {
        try {
            return objectMapper.readValue(record.value(), AppointmentCreatedEvent.class);
        } catch (Exception malformed) {
            throw new IllegalStateException("Could not read event from " + record.topic(), malformed);
        }
    }
}
