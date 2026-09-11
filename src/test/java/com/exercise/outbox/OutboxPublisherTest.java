package com.exercise.outbox;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.exercise.AbstractIntegrationTest;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.support.TransactionTemplate;

class OutboxPublisherTest extends AbstractIntegrationTest {

    @Autowired
    private OutboxPublisher poller;

    @Autowired
    private TransactionTemplate transactions;

    @Autowired
    private EntityManager entityManager;

    @MockitoBean
    private EventPublisher broker;

    private final List<UUID> delivered = new ArrayList<>();

    @BeforeEach
    void recordWhatIsPublished() {
        delivered.clear();
        org.mockito.BDDMockito.willAnswer(invocation -> {
                    delivered.add(invocation.getArgument(0));
                    return null;
                })
                .given(broker)
                .publish(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
                        org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @AfterEach
    void emptyTheOutbox() {
        transactions.executeWithoutResult(status ->
                entityManager.createNativeQuery("TRUNCATE outbox_event").executeUpdate());
    }

    private UUID given(String createdAt, boolean alreadyPublished) {
        UUID id = UUID.randomUUID();
        transactions.executeWithoutResult(status -> entityManager
                .createNativeQuery("INSERT INTO outbox_event"
                        + " (id, domain_key, event_type, payload, created_at, published_at)"
                        + " VALUES (:id, '42', 'appointment_created', CAST('{}' AS jsonb), :createdAt,"
                        + " :publishedAt)")
                .setParameter("id", id)
                .setParameter("createdAt", Instant.parse(createdAt))
                .setParameter("publishedAt", alreadyPublished ? Instant.parse("2027-01-01T00:00:00Z") : null)
                .executeUpdate());
        return id;
    }

    private Instant publishedAtOf(UUID id) {
        return transactions.execute(status -> (Instant) entityManager
                .createNativeQuery("SELECT published_at FROM outbox_event WHERE id = :id")
                .setParameter("id", id)
                .getSingleResult());
    }

    @Test
    void publishesPendingEventsAndStampsThem() {
        UUID pending = given("2027-01-05T10:00:00Z", false);

        assertThat(poller.publishPending()).isEqualTo(1);

        assertThat(delivered).containsExactly(pending);
        assertThat(publishedAtOf(pending)).isNotNull();
    }

    @Test
    void leavesAlreadyPublishedEventsAlone() {
        given("2027-01-05T10:00:00Z", true);

        assertThat(poller.publishPending()).isZero();
        assertThat(delivered).isEmpty();
    }

    @Test
    void publishesInTheOrderTheEventsWereWritten() {
        UUID third = given("2027-01-05T12:00:00Z", false);
        UUID first = given("2027-01-05T10:00:00Z", false);
        UUID second = given("2027-01-05T11:00:00Z", false);

        poller.publishPending();

        assertThat(delivered).containsExactly(first, second, third);
    }

    @Test
    void anEventTheBrokerRefusesStaysPending() {
        UUID pending = given("2027-01-05T10:00:00Z", false);
        org.mockito.BDDMockito.willThrow(new IllegalStateException("broker is down"))
                .given(broker)
                .publish(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
                        org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());

        assertThatThrownBy(() -> poller.publishPending()).isInstanceOf(IllegalStateException.class);

        assertThat(publishedAtOf(pending)).describedAs("nothing was sent, so nothing is marked").isNull();
    }
}
