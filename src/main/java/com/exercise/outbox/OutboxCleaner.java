package com.exercise.outbox;

import com.exercise.config.OutboxProperties;
import java.time.Clock;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class OutboxCleaner {

    private static final Logger log = LoggerFactory.getLogger(OutboxCleaner.class);

    private final OutboxEventRepository events;
    private final OutboxProperties properties;
    private final Clock clock;

    OutboxCleaner(OutboxEventRepository events, OutboxProperties properties, Clock clock) {
        this.events = events;
        this.properties = properties;
        this.clock = clock;
    }

    @Scheduled(fixedDelayString = "${outbox.cleanup-interval}")
    @Transactional
    public int deleteEventsPublishedLongAgo() {
        Instant cutoff = clock.instant().minus(properties.retention());
        int deleted = events.deletePublishedBefore(cutoff);

        if (deleted > 0) {
            log.info("removed {} outbox events published before {}", deleted, cutoff);
        }
        return deleted;
    }
}
