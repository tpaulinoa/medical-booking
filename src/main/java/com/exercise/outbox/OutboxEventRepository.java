package com.exercise.outbox;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {

    @Query(
            value =
                    """
                    SELECT * FROM outbox_event
                    WHERE published_at IS NULL
                    ORDER BY created_at
                    LIMIT :batchSize
                    FOR UPDATE SKIP LOCKED
                    """,
            nativeQuery = true)
    List<OutboxEvent> lockUnpublished(@Param("batchSize") int batchSize);

    /** Unpublished rows have a null published_at, so they never match and are never deleted. */
    @Modifying
    @Query(value = "DELETE FROM outbox_event WHERE published_at < :cutoff", nativeQuery = true)
    int deletePublishedBefore(@Param("cutoff") Instant cutoff);
}
