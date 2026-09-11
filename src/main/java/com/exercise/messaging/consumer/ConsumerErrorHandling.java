package com.exercise.messaging.consumer;

import com.exercise.config.KafkaConsumerProperties;
import com.exercise.messaging.AppointmentEventsTopic;
import java.nio.charset.StandardCharsets;
import org.apache.kafka.common.header.Header;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.ExponentialBackOff;

/**
 * Retries a failing action with backoff. After the last attempt the event is logged and skipped, so one bad event
 * does not block the rest of the partition.
 */
@Configuration
class ConsumerErrorHandling {

    private static final Logger log = LoggerFactory.getLogger(ConsumerErrorHandling.class);

    @Bean
    CommonErrorHandler postBookingErrorHandler(KafkaConsumerProperties properties) {
        ExponentialBackOff backOff = new ExponentialBackOff(
                properties.initialBackoff().toMillis(), properties.backoffMultiplier());
        backOff.setMaxAttempts(properties.maxAttempts() - 1);

        DefaultErrorHandler handler = new DefaultErrorHandler(
                (record, exception) -> log.error(
                        "giving up on event={} after {} attempts; the action did not happen",
                        eventIdOf(record.headers().lastHeader(AppointmentEventsTopic.EVENT_ID_HEADER)),
                        properties.maxAttempts(),
                        exception),
                backOff);

        handler.setLogLevel(org.springframework.kafka.KafkaException.Level.DEBUG);
        return handler;
    }

    private static String eventIdOf(Header header) {
        return header == null ? "<no event_id header>" : new String(header.value(), StandardCharsets.UTF_8);
    }
}
