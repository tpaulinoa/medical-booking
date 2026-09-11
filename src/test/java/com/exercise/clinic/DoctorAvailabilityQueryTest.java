package com.exercise.clinic;

import static org.assertj.core.api.Assertions.assertThat;

import com.exercise.AbstractRepositoryTest;
import com.exercise.TestClinic;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class DoctorAvailabilityQueryTest extends AbstractRepositoryTest {

    private static final Instant SLOT = Instant.parse("2027-01-05T08:30:00Z");
    private static final Instant DAY_START = Instant.parse("2027-01-04T23:00:00Z");
    private static final Instant DAY_END = Instant.parse("2027-01-05T23:00:00Z");

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
    private Speciality dermatology;

    @BeforeEach
    void buildTheClinic() {
        clinic = new TestClinic(doctors, rooms, patients, specialities);
        cardiology = clinic.speciality("Cardiologia");
        dermatology = clinic.speciality("Dermatologia");
    }

    private List<Long> candidates(Speciality speciality, String dayOfWeek, String from, String to) {
        return doctors.findAvailableDoctorIds(
                speciality.getId(),
                dayOfWeek,
                LocalTime.parse(from),
                LocalTime.parse(to),
                SLOT,
                DAY_START,
                DAY_END);
    }

    @Test
    void returnsOnlyDoctorsHoldingTheRequestedSpeciality() {
        Doctor cardiologist = clinic.doctor(cardiology);
        clinic.doctor(dermatology);

        assertThat(candidates(cardiology, "TUESDAY", "09:30", "10:00"))
                .containsExactly(cardiologist.getId());
    }

    @Test
    void excludesDoctorsWhoseWorkingHoursDoNotCoverTheWholeSlot() {
        Doctor morningOnly =
                clinic.doctor(LocalTime.of(8, 0), LocalTime.of(14, 0), TestClinic.WEEKDAYS, cardiology);

        assertThat(candidates(cardiology, "TUESDAY", "07:00", "07:30")).isEmpty();
        assertThat(candidates(cardiology, "TUESDAY", "13:30", "14:00")).containsExactly(morningOnly.getId());
        assertThat(candidates(cardiology, "TUESDAY", "14:00", "14:30")).isEmpty();
        // 23:30 to midnight: the end of the slot wraps around to 00:00
        assertThat(candidates(cardiology, "TUESDAY", "23:30", "00:00")).isEmpty();
    }

    @Test
    void excludesDoctorsWhoDoNotWorkThatDayOfTheWeek() {
        Doctor tuesdayToSaturday = clinic.doctor(
                LocalTime.of(9, 0),
                LocalTime.of(18, 0),
                Set.of(
                        DayOfWeek.TUESDAY,
                        DayOfWeek.WEDNESDAY,
                        DayOfWeek.THURSDAY,
                        DayOfWeek.FRIDAY,
                        DayOfWeek.SATURDAY),
                cardiology);

        assertThat(candidates(cardiology, "MONDAY", "09:30", "10:00")).isEmpty();
        assertThat(candidates(cardiology, "SATURDAY", "09:30", "10:00"))
                .containsExactly(tuesdayToSaturday.getId());
    }

    @Test
    void theLockingQueryPicksOneOfTheSameCandidates() {
        clinic.doctor(cardiology);
        clinic.doctor(cardiology);

        List<Long> all = candidates(cardiology, "TUESDAY", "09:30", "10:00");
        Optional<Long> locked = doctors.lockNextAvailableDoctorId(
                cardiology.getId(),
                "TUESDAY",
                LocalTime.parse("09:30"),
                LocalTime.parse("10:00"),
                SLOT,
                DAY_START,
                DAY_END);

        assertThat(all).hasSize(2);
        assertThat(locked).isPresent();
        assertThat(all).contains(locked.orElseThrow());
    }

    @Test
    void theLockingQueryFindsNothingWhenThereAreNoCandidates() {
        assertThat(doctors.lockNextAvailableDoctorId(
                        cardiology.getId(),
                        "TUESDAY",
                        LocalTime.parse("09:30"),
                        LocalTime.parse("10:00"),
                        SLOT,
                        DAY_START,
                        DAY_END))
                .isEmpty();
    }
}
