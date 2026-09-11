package com.exercise.appointment;

import static org.assertj.core.api.Assertions.assertThat;

import com.exercise.AbstractIntegrationTest;
import com.exercise.TestClinic;
import com.exercise.FrozenClock;
import com.exercise.clinic.DoctorRepository;
import com.exercise.clinic.PatientRepository;
import com.exercise.clinic.RoomRepository;
import com.exercise.clinic.Speciality;
import com.exercise.clinic.SpecialityRepository;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Not a @DataJpaTest: that rolls back each test, and the threads would not see each other's writes. Here every
 * booking commits, so the data is removed after each test.
 */
@Import(FrozenClock.class)
class ConcurrentBookingTest extends AbstractIntegrationTest {

    private static final Instant SLOT = Instant.parse("2027-01-06T10:00:00Z");
    private static final int ATTEMPTS = 8;

    @Autowired
    private AppointmentService service;

    @Autowired
    private PatientRepository patients;

    @Autowired
    private SpecialityRepository specialities;

    @Autowired
    private DoctorRepository doctors;

    @Autowired
    private RoomRepository rooms;

    private Speciality onlyOneDoctor;
    private Speciality plentyOfDoctors;
    private List<Long> patientIds;

    @Autowired
    private TransactionTemplate transactions;

    @Autowired
    private EntityManager entityManager;

    @BeforeEach
    void buildTheClinic() {
        TestClinic clinic = new TestClinic(doctors, rooms, patients, specialities);

        onlyOneDoctor = clinic.speciality("Urologia");
        clinic.doctor(onlyOneDoctor);

        plentyOfDoctors = clinic.speciality("Medicina Geral e Familiar");
        for (int i = 0; i < ATTEMPTS; i++) {
            clinic.doctor(plentyOfDoctors);
            clinic.room();
        }

        patientIds = new ArrayList<>();
        for (int i = 0; i < ATTEMPTS; i++) {
            patientIds.add(clinic.patient().getId());
        }
    }

    @AfterEach
    void emptyTheClinic() {
        transactions.executeWithoutResult(status -> entityManager
                .createNativeQuery("TRUNCATE appointment, doctor_speciality, doctor_working_day,"
                        + " doctor, room, patient, speciality, outbox_event, processed_event"
                        + " RESTART IDENTITY CASCADE")
                .executeUpdate());
    }

    private record Outcome(Long appointmentId, BookingError error) {}

    /** The latch releases all threads at once, so the bookings really run at the same time. */
    private List<Outcome> raceFor(List<Long> patientIds, Long specialityId) throws Exception {
        CountDownLatch startLine = new CountDownLatch(1);
        List<Future<Outcome>> futures = new ArrayList<>();
        ExecutorService pool = Executors.newFixedThreadPool(ATTEMPTS);

        try {
            for (Long patientId : patientIds) {
                futures.add(pool.submit(() -> {
                    startLine.await();
                    try {
                        return new Outcome(service.book(patientId, specialityId, SLOT).getId(), null);
                    } catch (BookingException rejected) {
                        // A BookingException is an expected result. Anything else fails the test.
                        return new Outcome(null, rejected.error());
                    }
                }));
            }

            startLine.countDown();
            pool.shutdown();
            assertThat(pool.awaitTermination(60, TimeUnit.SECONDS)).isTrue();
        } finally {
            pool.shutdownNow();
        }

        List<Outcome> outcomes = new ArrayList<>();
        for (Future<Outcome> future : futures) {
            outcomes.add(future.get());
        }
        return outcomes;
    }

    private long appointmentsAt(Instant slot) {
        return transactions.execute(status -> ((Number) entityManager
                        .createNativeQuery("SELECT count(*) FROM appointment WHERE start_time = :slot")
                        .setParameter("slot", slot)
                        .getSingleResult())
                .longValue());
    }

    private List<Long> booked(List<Outcome> outcomes) {
        return outcomes.stream().map(Outcome::appointmentId).filter(Objects::nonNull).toList();
    }

    private List<BookingError> rejected(List<Outcome> outcomes) {
        return outcomes.stream().map(Outcome::error).filter(Objects::nonNull).toList();
    }

    /**
     * One doctor, so the slot can be booked only once. The others get TEMPORARILY_UNAVAILABLE, not
     * NO_DOCTOR_AVAILABLE, because the doctor exists and was only locked.
     */
    @Test
    void onlyOneOfManySimultaneousRequestsGetsTheSingleUrologist() throws Exception {
        List<Outcome> outcomes = raceFor(patientIds, onlyOneDoctor.getId());

        assertThat(booked(outcomes)).hasSize(1);
        assertThat(rejected(outcomes))
                .hasSize(ATTEMPTS - 1)
                .allSatisfy(error -> assertThat(error)
                        .isIn(
                                BookingError.NO_DOCTOR_AVAILABLE,
                                BookingError.TEMPORARILY_UNAVAILABLE,
                                BookingError.SLOT_TAKEN));
        assertThat(appointmentsAt(SLOT)).isEqualTo(1L);
    }

    /**
     * Same patient and slot, sent eight times, with enough doctors. Only the patient can limit the bookings, and
     * every other request gets PATIENT_ALREADY_BOOKED.
     */
    @Test
    void aDoubleSubmitBooksThePatientOnlyOnce() throws Exception {
        List<Outcome> outcomes =
                raceFor(Collections.nCopies(ATTEMPTS, patientIds.get(0)), plentyOfDoctors.getId());

        assertThat(booked(outcomes)).hasSize(1);
        assertThat(rejected(outcomes))
                .hasSize(ATTEMPTS - 1)
                .containsOnly(BookingError.PATIENT_ALREADY_BOOKED);
        assertThat(appointmentsAt(SLOT)).isEqualTo(1L);
    }
}
