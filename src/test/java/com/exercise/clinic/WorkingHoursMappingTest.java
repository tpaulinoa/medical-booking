package com.exercise.clinic;

import static org.assertj.core.api.Assertions.assertThat;

import com.exercise.AbstractRepositoryTest;
import com.exercise.TestClinic;
import jakarta.persistence.EntityManager;
import java.time.LocalTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/** A global time zone setting in Hibernate once shifted TIME columns (08:00 read as 09:00). */
class WorkingHoursMappingTest extends AbstractRepositoryTest {

    @Autowired
    private DoctorRepository doctors;

    @Autowired
    private RoomRepository rooms;

    @Autowired
    private PatientRepository patients;

    @Autowired
    private SpecialityRepository specialities;

    @Autowired
    private EntityManager entityManager;

    @Test
    void workingHoursComeBackExactlyAsTheyWereStored() {
        TestClinic clinic = new TestClinic(doctors, rooms, patients, specialities);
        Long id = clinic.doctor(LocalTime.of(8, 0), LocalTime.of(14, 0), TestClinic.WEEKDAYS)
                .getId();

        entityManager.clear();
        Doctor reloaded = doctors.findById(id).orElseThrow();

        assertThat(reloaded.getWorkingHoursStart()).isEqualTo(LocalTime.of(8, 0));
        assertThat(reloaded.getWorkingHoursEnd()).isEqualTo(LocalTime.of(14, 0));
    }
}
