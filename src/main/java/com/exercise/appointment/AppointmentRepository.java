package com.exercise.appointment;

import java.time.Instant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    boolean existsByPatientIdAndStartTime(Long patientId, Instant startTime);

    @Override
    @EntityGraph(attributePaths = {"patient", "doctor", "room", "speciality"})
    Page<Appointment> findAll(Pageable pageable);
}
