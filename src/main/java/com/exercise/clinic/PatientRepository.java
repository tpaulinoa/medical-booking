package com.exercise.clinic;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

public interface PatientRepository extends JpaRepository<Patient, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Patient> findLockedById(Long id);
}
