package com.exercise.outbox;

import java.util.UUID;

public interface EventPublisher {

    void publish(UUID eventId, String domainKey, String eventType, String payload);
}
