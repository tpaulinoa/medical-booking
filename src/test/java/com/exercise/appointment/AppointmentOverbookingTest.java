package com.exercise.appointment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

class AppointmentOverbookingTest extends AbstractRepositoryTest {

    private static final Instant SLOT = Instant.parse("2027-01-04T09:00:00Z");

    @Autowired
    private AppointmentRepository appointments;

    @Autowired
    private DoctorRepository doctors;

    @Autowired
    private RoomRepository rooms;

    @Autowired
    private PatientRepository patients;

    @Autowired
    private SpecialityRepository specialities;

    private List<Doctor> threeDoctors;
    private List<Room> threeRooms;
    private List<Patient> threePatients;
    private Speciality speciality;

    @BeforeEach
    void buildAClinicWithThreeOfEverything() {
        TestClinic clinic = new TestClinic(doctors, rooms, patients, specialities);
        speciality = clinic.speciality("Cardiologia");
        threeDoctors = List.of(clinic.doctor(speciality), clinic.doctor(speciality), clinic.doctor(speciality));
        threeRooms = List.of(clinic.room(), clinic.room(), clinic.room());
        threePatients = List.of(clinic.patient(), clinic.patient(), clinic.patient());
    }

    private Appointment booking(int patient, int doctor, int room) {
        return new Appointment(
                threePatients.get(patient), threeDoctors.get(doctor), threeRooms.get(room), speciality, SLOT);
    }

    /** Makes sure the other tests fail because of the constraint, not because every insert fails. */
    @Test
    void bookingsAtTheSameTimeSucceedWhenDoctorRoomAndPatientAllDiffer() {
        for (int i = 0; i < 3; i++) {
            appointments.saveAndFlush(booking(i, i, i));
        }

        assertThat(appointments.findAll())
                .filteredOn(appointment -> appointment.getStartTime().equals(SLOT))
                .hasSize(3);
    }

    @Test
    void theSameDoctorCannotBeBookedTwiceAtTheSameTime() {
        appointments.saveAndFlush(booking(0, 0, 0));

        assertThatThrownBy(() -> appointments.saveAndFlush(booking(1, 0, 1)))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasStackTraceContaining("uq_appointment_doctor_slot");
    }

    @Test
    void theSameRoomCannotBeBookedTwiceAtTheSameTime() {
        appointments.saveAndFlush(booking(0, 0, 0));

        assertThatThrownBy(() -> appointments.saveAndFlush(booking(1, 1, 0)))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasStackTraceContaining("uq_appointment_room_slot");
    }

    @Test
    void theSamePatientCannotBeBookedTwiceAtTheSameTime() {
        appointments.saveAndFlush(booking(0, 0, 0));

        assertThatThrownBy(() -> appointments.saveAndFlush(booking(0, 1, 1)))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasStackTraceContaining("uq_appointment_patient_slot");
    }
}
