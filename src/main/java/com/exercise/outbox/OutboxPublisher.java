package com.exercise.outbox;

import com.exercise.config.OutboxProperties;
import java.time.Clock;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class OutboxPublisher {

    private static final Logger log = LoggerFactory.getLogger(OutboxPublisher.class);

    private final OutboxEventRepository events;
    private final EventPublisher publisher;
    private final OutboxProperties properties;
    private final Clock clock;

    OutboxPublisher(
            OutboxEventRepository events,
            EventPublisher publisher,
            OutboxProperties properties,
            Clock clock) {
        this.events = events;
        this.publisher = publisher;
        this.properties = properties;
        this.clock = clock;
    }

    @Scheduled(fixedDelayString = "${outbox.poll-interval}")
    @Transactional
    public int publishPending() {
        List<OutboxEvent> batch = events.lockUnpublished(properties.batchSize());
        if (batch.isEmpty()) {
            return 0;
        }

        for (OutboxEvent event : batch) {
            // Send first, then mark as published: a crash in between sends the event again, which the consumers
            // ignore, instead of losing it.
            try {
                publisher.publish(event.getId(), event.getDomainKey(), event.getEventType(), event.getPayload());
            } catch (RuntimeException failure) {
                log.warn("could not publish event {}", event.getId());
                throw failure;
            }
            event.markPublished(clock.instant());
        }

        log.debug("published {} events", batch.size());
        return batch.size();
    }
}
