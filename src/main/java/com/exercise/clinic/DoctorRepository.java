package com.exercise.clinic;

import java.time.Instant;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DoctorRepository extends JpaRepository<Doctor, Long> {

    String AVAILABLE_DOCTORS =
            """
            SELECT d.id
            FROM doctor d
            WHERE EXISTS (SELECT 1 FROM doctor_speciality ds
                          WHERE ds.doctor_id = d.id AND ds.speciality_id = :specialityId)
              AND EXISTS (SELECT 1 FROM doctor_working_day w
                          WHERE w.doctor_id = d.id AND w.day_of_week = :dayOfWeek)
              AND d.working_hours_start <= :localStart
              AND d.working_hours_end >= :localEnd
              AND :localEnd > :localStart
              AND NOT EXISTS (SELECT 1 FROM appointment a
                              WHERE a.doctor_id = d.id AND a.start_time = :startTime)
            ORDER BY (SELECT count(*) FROM appointment load
                      WHERE load.doctor_id = d.id
                        AND load.start_time >= :dayStart
                        AND load.start_time < :dayEnd),
                     random()
            """;

    @Query(value = AVAILABLE_DOCTORS, nativeQuery = true)
    List<Long> findAvailableDoctorIds(
            @Param("specialityId") Long specialityId,
            @Param("dayOfWeek") String dayOfWeek,
            @Param("localStart") LocalTime localStart,
            @Param("localEnd") LocalTime localEnd,
            @Param("startTime") Instant startTime,
            @Param("dayStart") Instant dayStart,
            @Param("dayEnd") Instant dayEnd);

    @Query(value = AVAILABLE_DOCTORS + " LIMIT 1 FOR UPDATE OF d SKIP LOCKED", nativeQuery = true)
    Optional<Long> lockNextAvailableDoctorId(
            @Param("specialityId") Long specialityId,
            @Param("dayOfWeek") String dayOfWeek,
            @Param("localStart") LocalTime localStart,
            @Param("localEnd") LocalTime localEnd,
            @Param("startTime") Instant startTime,
            @Param("dayStart") Instant dayStart,
            @Param("dayEnd") Instant dayEnd);
}
