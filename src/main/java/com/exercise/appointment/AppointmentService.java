package com.exercise.appointment;

import com.exercise.clinic.DoctorRepository;
import com.exercise.clinic.Patient;
import com.exercise.clinic.PatientRepository;
import com.exercise.clinic.RoomRepository;
import com.exercise.clinic.Speciality;
import com.exercise.clinic.SpecialityRepository;
import com.exercise.messaging.AppointmentCreatedEvent;
import com.exercise.outbox.OutboxWriter;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AppointmentService {

    private static final Logger log = LoggerFactory.getLogger(AppointmentService.class);

    private final AppointmentRepository appointments;
    private final DoctorRepository doctors;
    private final RoomRepository rooms;
    private final PatientRepository patients;
    private final SpecialityRepository specialities;
    private final SlotGrid slotGrid;
    private final OutboxWriter outbox;
    private final Clock clock;

    AppointmentService(
            AppointmentRepository appointments,
            DoctorRepository doctors,
            RoomRepository rooms,
            PatientRepository patients,
            SpecialityRepository specialities,
            SlotGrid slotGrid,
            OutboxWriter outbox,
            Clock clock) {
        this.appointments = appointments;
        this.doctors = doctors;
        this.rooms = rooms;
        this.patients = patients;
        this.specialities = specialities;
        this.slotGrid = slotGrid;
        this.outbox = outbox;
        this.clock = clock;
    }

    @Transactional
    public Appointment book(Long patientId, Long specialityId, Instant startTime) {
        if (!startTime.isAfter(clock.instant())) {
            throw new BookingException(BookingError.START_TIME_IN_THE_PAST);
        }
        if (!slotGrid.isOnGrid(startTime)) {
            throw new BookingException(BookingError.START_TIME_NOT_ON_GRID);
        }

        // Locks the patient, so concurrent requests for the same patient run one after the other.
        Patient patient = patients.findLockedById(patientId)
                .orElseThrow(() -> new BookingException(BookingError.PATIENT_NOT_FOUND));
        Speciality speciality = specialities.findById(specialityId)
                .orElseThrow(() -> new BookingException(BookingError.SPECIALITY_NOT_FOUND));
        if (appointments.existsByPatientIdAndStartTime(patientId, startTime)) {
            throw new BookingException(BookingError.PATIENT_ALREADY_BOOKED);
        }

        Long doctorId = claimDoctor(specialityId, startTime);
        Long roomId = claimRoom(startTime);

        Appointment appointment = new Appointment(
                patient,
                doctors.findById(doctorId).orElseThrow(),
                rooms.findById(roomId).orElseThrow(),
                speciality,
                startTime);

        Appointment booked;
        try {
            booked = appointments.saveAndFlush(appointment);
        } catch (DataIntegrityViolationException violation) {
            String cause = String.valueOf(violation.getMostSpecificCause().getMessage());
            if (cause.contains("uq_appointment_patient_slot")) {
                throw new BookingException(BookingError.PATIENT_ALREADY_BOOKED);
            }
            if (cause.contains("uq_appointment_doctor_slot") || cause.contains("uq_appointment_room_slot")) {
                throw new BookingException(BookingError.SLOT_TAKEN);
            }
            throw violation;
        }

        UUID eventId = UUID.randomUUID();
        outbox.write(
                eventId,
                String.valueOf(booked.getId()),
                AppointmentCreatedEvent.TYPE,
                AppointmentCreatedEvent.of(eventId, clock.instant(), booked));

        log.info(
                "booked appointment {}: patient {}, doctor {}, room {}, {}",
                booked.getId(),
                patientId,
                doctorId,
                roomId,
                startTime);
        return booked;
    }

    @Transactional(readOnly = true)
    public Page<Appointment> list(int page, int size) {
        return appointments.findAll(PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id")));
    }

    private Long claimDoctor(Long specialityId, Instant startTime) {
        String dayOfWeek = slotGrid.dayOfWeek(startTime).name();
        return doctors.lockNextAvailableDoctorId(
                        specialityId,
                        dayOfWeek,
                        slotGrid.localTime(startTime),
                        slotGrid.localEndTime(startTime),
                        startTime,
                        slotGrid.startOfDay(startTime),
                        slotGrid.startOfNextDay(startTime))
                .orElseThrow(() -> new BookingException(
                        doctors.findAvailableDoctorIds(
                                                specialityId,
                                                dayOfWeek,
                                                slotGrid.localTime(startTime),
                                                slotGrid.localEndTime(startTime),
                                                startTime,
                                                slotGrid.startOfDay(startTime),
                                                slotGrid.startOfNextDay(startTime))
                                        .isEmpty()
                                ? BookingError.NO_DOCTOR_AVAILABLE
                                : BookingError.TEMPORARILY_UNAVAILABLE));
    }

    private Long claimRoom(Instant startTime) {
        return rooms.lockNextAvailableRoomId(startTime)
                .orElseThrow(() -> new BookingException(
                        rooms.findAvailableRoomIds(startTime).isEmpty()
                                ? BookingError.NO_ROOM_AVAILABLE
                                : BookingError.TEMPORARILY_UNAVAILABLE));
    }
}
