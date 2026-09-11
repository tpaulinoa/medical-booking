package com.exercise.outbox;

import static org.assertj.core.api.Assertions.assertThat;

import com.exercise.AbstractIntegrationTest;
import com.exercise.FrozenClock;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.support.TransactionTemplate;

/** Retention is 30 days from FrozenClock.NOW, so anything published before 2026-12-05 is removed. */
@Import(FrozenClock.class)
class OutboxCleanerTest extends AbstractIntegrationTest {

    private static final Instant LONG_AGO = Instant.parse("2026-11-01T00:00:00Z");
    private static final Instant RECENTLY = Instant.parse("2027-01-02T00:00:00Z");

    @Autowired
    private OutboxCleaner cleaner;

    @Autowired
    private TransactionTemplate transactions;

    @Autowired
    private EntityManager entityManager;

    @AfterEach
    void emptyTheOutbox() {
        transactions.executeWithoutResult(status ->
                entityManager.createNativeQuery("TRUNCATE outbox_event").executeUpdate());
    }

    private UUID given(Instant createdAt, Instant publishedAt) {
        UUID id = UUID.randomUUID();
        transactions.executeWithoutResult(status -> entityManager
                .createNativeQuery("INSERT INTO outbox_event"
                        + " (id, domain_key, event_type, payload, created_at, published_at)"
                        + " VALUES (:id, '42', 'appointment_created', CAST('{}' AS jsonb), :createdAt,"
                        + " :publishedAt)")
                .setParameter("id", id)
                .setParameter("createdAt", createdAt)
                .setParameter("publishedAt", publishedAt)
                .executeUpdate());
        return id;
    }

    private boolean stillThere(UUID id) {
        return transactions.execute(status -> ((Number) entityManager
                                .createNativeQuery("SELECT count(*) FROM outbox_event WHERE id = :id")
                                .setParameter("id", id)
                                .getSingleResult())
                        .intValue()
                == 1);
    }

    @Test
    void removesOnlyWhatIsOldEnough() {
        UUID old = given(LONG_AGO, LONG_AGO);
        UUID recent = given(RECENTLY, RECENTLY);
        UUID unsent = given(LONG_AGO, null);

        assertThat(cleaner.deleteEventsPublishedLongAgo()).isEqualTo(1);

        assertThat(stillThere(old)).isFalse();
        assertThat(stillThere(recent)).isTrue();
        assertThat(stillThere(unsent)).isTrue();
    }
}
