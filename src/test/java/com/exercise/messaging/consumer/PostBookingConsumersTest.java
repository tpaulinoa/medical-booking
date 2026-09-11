package com.exercise.messaging.consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.exercise.AbstractKafkaTest;
import com.exercise.config.KafkaTopicProperties;
import com.exercise.integration.calendar.CalendarClient;
import com.exercise.integration.email.NotificationSender;
import com.exercise.integration.roomreservation.RoomReservationClient;
import com.exercise.messaging.AppointmentCreatedEvent;
import com.exercise.outbox.EventPublisher;
import jakarta.persistence.EntityManager;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.BDDMockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.support.TransactionTemplate;

class PostBookingConsumersTest extends AbstractKafkaTest {

    @Autowired
    private EventPublisher publisher;

    @Autowired
    private KafkaTopicProperties topic;

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

    private static String payload(UUID eventId, long appointmentId) {
        return """
                {"event_id":"%s","event_type":"appointment_created",
                 "occurred_at":"2026-09-10T08:00:00Z","version":1,
                 "appointment_id":%d,"start_time":"2026-09-15T13:30:00Z",
                 "speciality":{"id":2,"name":"Cardiologia"},
                 "doctor":{"id":7,"name":"Luis Teixeira","email":"luis@example.com"},
                 "patient":{"id":1,"name":"Miguel Sousa","email":"miguel@example.com"},
                 "room":{"id":4,"number":"Sala 4"}}
                """
                .formatted(eventId, appointmentId);
    }

    private long processedRows(UUID... eventIds) {
        return transactions.execute(status -> ((Number) entityManager
                        .createNativeQuery("SELECT count(*) FROM processed_event WHERE event_id IN (:ids)")
                        .setParameter("ids", List.of(eventIds))
                        .getSingleResult())
                .longValue());
    }

    @Test
    void everyPostBookingActionHappensOnceForOneEvent() {
        UUID eventId = UUID.randomUUID();

        publisher.publish(eventId, "42", AppointmentCreatedEvent.TYPE, payload(eventId, 42));

        // one row per consumer
        await().atMost(Duration.ofSeconds(30)).until(() -> processedRows(eventId) == 3L);

        BDDMockito.then(notifications).should().sendBookingConfirmation(BDDMockito.any());
        BDDMockito.then(calendar).should().addAppointment(BDDMockito.any());
        BDDMockito.then(rooms).should().reserve(BDDMockito.any());
    }

    /**
     * A third event with the same key lands on the same partition, after the duplicate. Once it is handled, the
     * duplicate was handled too.
     */
    @Test
    void anEventDeliveredTwiceActsOnlyOnce() {
        UUID duplicated = UUID.randomUUID();
        UUID afterwards = UUID.randomUUID();

        publisher.publish(duplicated, "77", AppointmentCreatedEvent.TYPE, payload(duplicated, 77));
        publisher.publish(duplicated, "77", AppointmentCreatedEvent.TYPE, payload(duplicated, 77));
        publisher.publish(afterwards, "77", AppointmentCreatedEvent.TYPE, payload(afterwards, 77));

        // two distinct events, three consumers
        await().atMost(Duration.ofSeconds(30)).until(() -> processedRows(duplicated, afterwards) == 6L);

        var received = org.mockito.ArgumentCaptor.forClass(AppointmentCreatedEvent.class);
        BDDMockito.then(notifications).should(BDDMockito.times(2)).sendBookingConfirmation(received.capture());
        assertThat(received.getAllValues())
                .map(AppointmentCreatedEvent::eventId)
                .describedAs("the duplicated event acted once, the later one once")
                .containsExactlyInAnyOrder(duplicated, afterwards);
    }
}
