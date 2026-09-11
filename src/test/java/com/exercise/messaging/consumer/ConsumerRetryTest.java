package com.exercise.messaging.consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.exercise.AbstractKafkaTest;
import com.exercise.integration.calendar.CalendarClient;
import com.exercise.integration.email.NotificationSender;
import com.exercise.integration.roomreservation.RoomReservationClient;
import com.exercise.messaging.AppointmentCreatedEvent;
import com.exercise.outbox.EventPublisher;
import jakarta.persistence.EntityManager;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.BDDMockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.support.TransactionTemplate;

class ConsumerRetryTest extends AbstractKafkaTest {

    @Autowired
    private EventPublisher publisher;

    @Autowired
    private TransactionTemplate transactions;

    @Autowired
    private EntityManager entityManager;

    @MockitoBean
    private NotificationSender notifications;

    @MockitoBean
    private CalendarClient calendar;

    @MockitoBean
    private RoomReservationClient rooms;

    @AfterEach
    void forgetWhatWasProcessed() {
        transactions.executeWithoutResult(status ->
                entityManager.createNativeQuery("TRUNCATE processed_event").executeUpdate());
    }

    private static String payload(UUID eventId) {
        return """
                {"event_id":"%s","event_type":"appointment_created",
                 "occurred_at":"2026-09-10T08:00:00Z","version":1,
                 "appointment_id":55,"start_time":"2026-09-15T13:30:00Z",
                 "speciality":{"id":2,"name":"Cardiologia"},
                 "doctor":{"id":7,"name":"Luis Teixeira","email":"luis@example.com"},
                 "patient":{"id":1,"name":"Miguel Sousa","email":"miguel@example.com"},
                 "room":{"id":4,"number":"Sala 4"}}
                """
                .formatted(eventId);
    }

    @Test
    void anActionThatFailsTwiceIsRetriedAndThenSucceedsOnce() {
        AtomicInteger attempts = new AtomicInteger();
        BDDMockito.willAnswer(invocation -> {
                    if (attempts.incrementAndGet() <= 2) {
                        throw new IllegalStateException("mail server is restarting");
                    }
                    return null;
                })
                .given(notifications)
                .sendBookingConfirmation(BDDMockito.any());

        UUID eventId = UUID.randomUUID();
        publisher.publish(eventId, "55", AppointmentCreatedEvent.TYPE, payload(eventId));

        await().atMost(Duration.ofSeconds(45)).untilAsserted(() -> assertThat(attempts.get()).isEqualTo(3));

        await().atMost(Duration.ofSeconds(20)).untilAsserted(() -> assertThat(notificationRows()).isEqualTo(1L));
    }

    @Test
    void anActionThatNeverSucceedsIsAbandonedAndTheNextEventStillGoesThrough() {
        UUID poisoned = UUID.randomUUID();
        UUID healthy = UUID.randomUUID();
        AtomicInteger attemptsOnPoisoned = new AtomicInteger();

        BDDMockito.willAnswer(invocation -> {
                    AppointmentCreatedEvent event = invocation.getArgument(0);
                    if (event.eventId().equals(poisoned)) {
                        attemptsOnPoisoned.incrementAndGet();
                        throw new IllegalStateException("this one never works");
                    }
                    return null;
                })
                .given(notifications)
                .sendBookingConfirmation(BDDMockito.any());

        // same key, same partition: the healthy event queues behind the poisoned one
        publisher.publish(poisoned, "66", AppointmentCreatedEvent.TYPE, payload(poisoned));
        publisher.publish(healthy, "66", AppointmentCreatedEvent.TYPE, payload(healthy));

        // only the healthy event gets recorded
        await().atMost(Duration.ofSeconds(60)).until(() -> notificationRows() == 1L);

        assertThat(attemptsOnPoisoned.get()).describedAs("max-attempts is 4").isEqualTo(4);
    }

    private long notificationRows() {
        return transactions.execute(status -> ((Number) entityManager
                        .createNativeQuery("SELECT count(*) FROM processed_event"
                                + " WHERE consumer_name = 'patient-notification'")
                        .getSingleResult())
                .longValue());
    }
}
