package com.exercise.outbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class OutboxWriter {

    private final EntityManager entityManager;
    private final ObjectMapper objectMapper;

    OutboxWriter(EntityManager entityManager, ObjectMapper objectMapper) {
        this.entityManager = entityManager;
        this.objectMapper = objectMapper;
    }

    public void write(UUID eventId, String domainKey, String eventType, Object payload) {
        String json;
        try {
            json = objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException cannotSerialise) {
            throw new IllegalStateException("Could not serialise " + eventType, cannotSerialise);
        }

        // persist instead of save: the id is set here, so save would run a select before the insert.
        entityManager.persist(new OutboxEvent(eventId, domainKey, eventType, json));
    }
}
