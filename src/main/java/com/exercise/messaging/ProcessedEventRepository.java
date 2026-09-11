package com.exercise.messaging;

import com.exercise.messaging.ProcessedEvent.ProcessedEventId;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

interface ProcessedEventRepository extends JpaRepository<ProcessedEvent, ProcessedEventId> {

    /**
     * Inserts the row unless it already exists. Returns 1 if it was inserted, 0 if another delivery recorded it
     * first.
     */
    @Modifying
    @Transactional
    @Query(
            value =
                    """
                    INSERT INTO processed_event (event_id, consumer_name, processed_at)
                    VALUES (:eventId, :consumerName, now())
                    ON CONFLICT DO NOTHING
                    """,
            nativeQuery = true)
    int recordIfAbsent(@Param("eventId") UUID eventId, @Param("consumerName") String consumerName);
}
