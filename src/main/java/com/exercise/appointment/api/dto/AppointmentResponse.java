package com.exercise.appointment.api.dto;

import com.exercise.appointment.Appointment;
import java.time.Instant;

public record AppointmentResponse(
        Long id,
        Instant startTime,
        String speciality,
        DoctorSummary doctor,
        RoomSummary room,
        PatientSummary patient) {

    public record DoctorSummary(Long id, String name, String email) {}

    public record RoomSummary(Long id, String number) {}

    public record PatientSummary(Long id, String name, String email) {}

    public static AppointmentResponse from(Appointment appointment) {
        return new AppointmentResponse(
                appointment.getId(),
                appointment.getStartTime(),
                appointment.getSpeciality().getName(),
                new DoctorSummary(
                        appointment.getDoctor().getId(),
                        appointment.getDoctor().getName(),
                        appointment.getDoctor().getEmail()),
                new RoomSummary(appointment.getRoom().getId(), appointment.getRoom().getRoomNumber()),
                new PatientSummary(
                        appointment.getPatient().getId(),
                        appointment.getPatient().getName(),
                        appointment.getPatient().getEmail()));
    }
}
