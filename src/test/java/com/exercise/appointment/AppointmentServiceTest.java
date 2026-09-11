package com.exercise.appointment;

import static org.assertj.core.api.Assertions.assertThat;

import com.exercise.AbstractRepositoryTest;
import com.exercise.TestClinic;
import com.exercise.FrozenClock;
import com.exercise.clinic.Doctor;
import com.exercise.clinic.DoctorRepository;
import com.exercise.clinic.Patient;
import com.exercise.clinic.PatientRepository;
import com.exercise.clinic.Room;
import com.exercise.clinic.RoomRepository;
import com.exercise.clinic.Speciality;
import com.exercise.clinic.SpecialityRepository;
import com.exercise.config.BookingProperties;
import com.exercise.outbox.OutboxWriter;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Import;

@Import({AppointmentService.class, SlotGrid.class, FrozenClock.class, OutboxWriter.class})
@ImportAutoConfiguration(JacksonAutoConfiguration.class)
@EnableConfigurationProperties(BookingProperties.class)
class AppointmentServiceTest extends AbstractRepositoryTest {

    private static final Instant SLOT = Instant.parse("2027-01-05T10:00:00Z");

    @Autowired
    private AppointmentService service;

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

    private TestClinic clinic;
    private Speciality cardiology;
    private Speciality ophthalmology;
    private List<Patient> somePatients;

    @BeforeEach
    void buildAClinicWithTwoCardiologistsAndNoOphthalmologist() {
        clinic = new TestClinic(doctors, rooms, patients, specialities);
        cardiology = clinic.speciality("Cardiologia");
        ophthalmology = clinic.speciality("Oftalmologia");

        clinic.doctor(cardiology);
        clinic.doctor(cardiology);
        clinic.room();
        clinic.room();

        somePatients = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            somePatients.add(clinic.patient());
        }
    }

    private BookingError errorOf(ThrowingCallable call) {
        try {
            call.call();
        } catch (BookingException expected) {
            return expected.error();
        } catch (Throwable other) {
            throw new AssertionError("expected a BookingException", other);
        }
        throw new AssertionError("expected a BookingException, nothing was thrown");
    }

    interface ThrowingCallable {
        void call() throws Exception;
    }

    @Test
    void booksAnAppointmentWithADoctorOfTheRequestedSpeciality() {
        Appointment booked = service.book(somePatients.get(0).getId(), cardiology.getId(), SLOT);

        assertThat(booked.getId()).isNotNull();
        assertThat(booked.getStartTime()).isEqualTo(SLOT);
        assertThat(booked.getSpeciality()).isEqualTo(cardiology);
        assertThat(booked.getDoctor().getSpecialities()).contains(cardiology);
    }

    @Test
    void rejectsATimeThatHasAlreadyPassed() {
        assertThat(errorOf(() -> service.book(
                        somePatients.get(0).getId(),
                        cardiology.getId(),
                        Instant.parse("2020-01-07T10:00:00Z"))))
                .isEqualTo(BookingError.START_TIME_IN_THE_PAST);
    }

    @Test
    void rejectsATimeThatDoesNotFallOnTheGrid() {
        assertThat(errorOf(() ->
                        service.book(somePatients.get(0).getId(), cardiology.getId(), SLOT.plusSeconds(600))))
                .isEqualTo(BookingError.START_TIME_NOT_ON_GRID);
    }

    @Test
    void rejectsASpecialityNobodyPractises() {
        assertThat(errorOf(() ->
                        service.book(somePatients.get(0).getId(), ophthalmology.getId(), SLOT)))
                .isEqualTo(BookingError.NO_DOCTOR_AVAILABLE);
    }

    @Test
    void rejectsAnUnknownPatient() {
        assertThat(errorOf(() -> service.book(999_999L, cardiology.getId(), SLOT)))
                .isEqualTo(BookingError.PATIENT_NOT_FOUND);
    }

    @Test
    void refusesToBookTheSamePatientTwiceAtTheSameTime() {
        service.book(somePatients.get(0).getId(), cardiology.getId(), SLOT);

        assertThat(errorOf(() -> service.book(somePatients.get(0).getId(), cardiology.getId(), SLOT)))
                .isEqualTo(BookingError.PATIENT_ALREADY_BOOKED);
    }

    /** The only doctor is busy with the patient's own appointment; the error must still be about the patient. */
    @Test
    void refusesTheSamePatientTwiceEvenWhenTheSpecialityHasOneDoctor() {
        Speciality urology = clinic.speciality("Urologia");
        clinic.doctor(urology);
        Long patientId = somePatients.get(0).getId();

        service.book(patientId, urology.getId(), SLOT);

        assertThat(errorOf(() -> service.book(patientId, urology.getId(), SLOT)))
                .isEqualTo(BookingError.PATIENT_ALREADY_BOOKED);
    }

    @Test
    void reportsWhenEveryRoomIsTakenEvenThoughADoctorIsFree() {
        Speciality other = clinic.speciality("Ortopedia");
        List<Room> allRooms = rooms.findAll();

        for (Room room : allRooms) {
            appointments.saveAndFlush(
                    new Appointment(clinic.patient(), clinic.doctor(other), room, other, SLOT));
        }

        assertThat(errorOf(() -> service.book(somePatients.get(0).getId(), cardiology.getId(), SLOT)))
                .isEqualTo(BookingError.NO_ROOM_AVAILABLE);
    }

    @Test
    void prefersTheDoctorWithFewerAppointmentsThatDay() {
        List<Doctor> cardiologists = doctors.findAll().stream()
                .filter(doctor -> doctor.getSpecialities().contains(cardiology))
                .toList();
        Doctor busy = cardiologists.get(0);
        Doctor idle = cardiologists.get(1);

        appointments.saveAndFlush(new Appointment(
                clinic.patient(), busy, clinic.room(), cardiology, SLOT.minusSeconds(3600)));

        Appointment booked = service.book(somePatients.get(0).getId(), cardiology.getId(), SLOT);

        assertThat(booked.getDoctor()).isEqualTo(idle);
    }

    @Test
    void listsTheNewestAppointmentsFirstWithoutRepeatingAnyAcrossPages() {
        Appointment first = service.book(somePatients.get(0).getId(), cardiology.getId(), SLOT);
        Appointment second = service.book(somePatients.get(1).getId(), cardiology.getId(), SLOT);
        Appointment third = service.book(somePatients.get(2).getId(), cardiology.getId(), SLOT.plusSeconds(1800));
        Appointment fourth = service.book(somePatients.get(3).getId(), cardiology.getId(), SLOT.plusSeconds(1800));

        assertThat(service.list(0, 2).getContent()).containsExactly(fourth, third);
        assertThat(service.list(1, 2).getContent()).containsExactly(second, first);
    }
}
