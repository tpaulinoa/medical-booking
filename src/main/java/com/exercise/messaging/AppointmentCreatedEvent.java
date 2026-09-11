package com.exercise.messaging;

import com.exercise.appointment.Appointment;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import java.time.Instant;
import java.util.UUID;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record AppointmentCreatedEvent(
        UUID eventId,
        String eventType,
        Instant occurredAt,
        int version,
        Long appointmentId,
        Instant startTime,
        SpecialitySummary speciality,
        DoctorSummary doctor,
        PatientSummary patient,
        RoomSummary room) {

    public static final String TYPE = "appointment_created";
    private static final int CURRENT_VERSION = 1;

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record DoctorSummary(Long id, String name, String email) {}

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record PatientSummary(Long id, String name, String email) {}

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record SpecialitySummary(Long id, String name) {}

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record RoomSummary(Long id, String number) {}

    public static AppointmentCreatedEvent of(UUID eventId, Instant occurredAt, Appointment appointment) {
        return new AppointmentCreatedEvent(
                eventId,
                TYPE,
                occurredAt,
                CURRENT_VERSION,
                appointment.getId(),
                appointment.getStartTime(),
                new SpecialitySummary(
                        appointment.getSpeciality().getId(), appointment.getSpeciality().getName()),
                new DoctorSummary(
                        appointment.getDoctor().getId(),
                        appointment.getDoctor().getName(),
                        appointment.getDoctor().getEmail()),
                new PatientSummary(
                        appointment.getPatient().getId(),
                        appointment.getPatient().getName(),
                        appointment.getPatient().getEmail()),
                new RoomSummary(appointment.getRoom().getId(), appointment.getRoom().getRoomNumber()));
    }
}
