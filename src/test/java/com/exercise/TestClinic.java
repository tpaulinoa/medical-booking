package com.exercise;

import com.exercise.clinic.Doctor;
import com.exercise.clinic.DoctorRepository;
import com.exercise.clinic.Patient;
import com.exercise.clinic.PatientRepository;
import com.exercise.clinic.Room;
import com.exercise.clinic.RoomRepository;
import com.exercise.clinic.Speciality;
import com.exercise.clinic.SpecialityRepository;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/** Creates the data each test needs. Unique columns use a counter, so repeated calls do not collide. */
public class TestClinic {

    public static final Set<DayOfWeek> WEEKDAYS = Set.of(
            DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY);

    private static final AtomicInteger SEQUENCE = new AtomicInteger();

    private final DoctorRepository doctors;
    private final RoomRepository rooms;
    private final PatientRepository patients;
    private final SpecialityRepository specialities;

    public TestClinic(
            DoctorRepository doctors,
            RoomRepository rooms,
            PatientRepository patients,
            SpecialityRepository specialities) {
        this.doctors = doctors;
        this.rooms = rooms;
        this.patients = patients;
        this.specialities = specialities;
    }

    private static int next() {
        return SEQUENCE.incrementAndGet();
    }

    public Speciality speciality(String name) {
        return specialities.saveAndFlush(new Speciality(name + " " + next(), null));
    }

    public Room room() {
        int id = next();
        return rooms.saveAndFlush(new Room("Sala " + id));
    }

    public Patient patient() {
        int id = next();
        return patients.saveAndFlush(new Patient(
                String.format("%09d", id),
                "Patient " + id,
                LocalDate.of(1990, 1, 1),
                "patient" + id + "@example.com",
                null));
    }

    public Doctor doctor(LocalTime from, LocalTime to, Set<DayOfWeek> days, Speciality... practises) {
        int id = next();
        Doctor doctor = new Doctor("Doctor " + id, "doctor" + id + "@example.com", from, to);
        days.forEach(doctor::addWorkingDay);
        for (Speciality speciality : practises) {
            doctor.addSpeciality(speciality);
        }
        return doctors.saveAndFlush(doctor);
    }

    public Doctor doctor(Speciality... practises) {
        return doctor(LocalTime.of(9, 0), LocalTime.of(18, 0), WEEKDAYS, practises);
    }
}
