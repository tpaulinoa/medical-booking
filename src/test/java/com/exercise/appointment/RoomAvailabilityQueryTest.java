package com.exercise.appointment;

import static org.assertj.core.api.Assertions.assertThat;

import com.exercise.AbstractRepositoryTest;
import com.exercise.TestClinic;
import com.exercise.clinic.Doctor;
import com.exercise.clinic.DoctorRepository;
import com.exercise.clinic.Patient;
import com.exercise.clinic.PatientRepository;
import com.exercise.clinic.Room;
import com.exercise.clinic.RoomRepository;
import com.exercise.clinic.Speciality;
import com.exercise.clinic.SpecialityRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class RoomAvailabilityQueryTest extends AbstractRepositoryTest {

    private static final Instant SLOT = Instant.parse("2027-02-02T09:00:00Z");
    private static final int ROOMS = 3;

    @Autowired
    private RoomRepository rooms;

    @Autowired
    private AppointmentRepository appointments;

    @Autowired
    private DoctorRepository doctors;

    @Autowired
    private PatientRepository patients;

    @Autowired
    private SpecialityRepository specialities;

    private List<Room> ownRooms;
    private List<Doctor> ownDoctors;
    private List<Patient> ownPatients;
    private Speciality speciality;

    @BeforeEach
    void buildAClinic() {
        TestClinic clinic = new TestClinic(doctors, rooms, patients, specialities);
        speciality = clinic.speciality("Cardiologia");
        ownRooms = new ArrayList<>();
        ownDoctors = new ArrayList<>();
        ownPatients = new ArrayList<>();
        for (int i = 0; i < ROOMS; i++) {
            ownRooms.add(clinic.room());
            ownDoctors.add(clinic.doctor(speciality));
            ownPatients.add(clinic.patient());
        }
    }

    private void book(int index) {
        appointments.saveAndFlush(new Appointment(
                ownPatients.get(index), ownDoctors.get(index), ownRooms.get(index), speciality, SLOT));
    }

    @Test
    void aRoomAlreadyBookedAtThatTimeDropsOutOfTheCandidates() {
        book(0);

        assertThat(rooms.findAvailableRoomIds(SLOT))
                .hasSize(ROOMS - 1)
                .doesNotContain(ownRooms.get(0).getId());
    }

    @Test
    void theSameRoomStaysAvailableForADifferentTime() {
        book(0);

        assertThat(rooms.findAvailableRoomIds(SLOT.plusSeconds(1800))).contains(ownRooms.get(0).getId());
    }

    @Test
    void theLockingQueryFindsNothingWhenEveryRoomIsBooked() {
        for (int i = 0; i < ROOMS; i++) {
            book(i);
        }

        assertThat(rooms.findAvailableRoomIds(SLOT)).isEmpty();
        assertThat(rooms.lockNextAvailableRoomId(SLOT)).isEmpty();
    }
}
