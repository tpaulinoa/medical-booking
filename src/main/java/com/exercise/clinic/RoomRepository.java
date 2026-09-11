package com.exercise.clinic;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RoomRepository extends JpaRepository<Room, Long> {

    String AVAILABLE_ROOMS =
            """
            SELECT r.id
            FROM room r
            WHERE NOT EXISTS (SELECT 1 FROM appointment a
                              WHERE a.room_id = r.id AND a.start_time = :startTime)
            ORDER BY r.id
            """;

    @Query(value = AVAILABLE_ROOMS, nativeQuery = true)
    List<Long> findAvailableRoomIds(@Param("startTime") Instant startTime);

    @Query(value = AVAILABLE_ROOMS + " LIMIT 1 FOR UPDATE OF r SKIP LOCKED", nativeQuery = true)
    Optional<Long> lockNextAvailableRoomId(@Param("startTime") Instant startTime);
}
