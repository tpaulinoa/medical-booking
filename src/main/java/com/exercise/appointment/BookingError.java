package com.exercise.appointment;

import org.springframework.http.HttpStatus;

public enum BookingError {
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "The request body is missing required fields or is malformed."),
    START_TIME_IN_THE_PAST(HttpStatus.BAD_REQUEST, "The requested time has already passed."),
    START_TIME_NOT_ON_GRID(HttpStatus.BAD_REQUEST, "Appointments start on a fixed slot boundary."),
    PATIENT_NOT_FOUND(HttpStatus.UNPROCESSABLE_ENTITY, "No patient is registered under that id."),
    SPECIALITY_NOT_FOUND(HttpStatus.UNPROCESSABLE_ENTITY, "No speciality is registered under that id."),
    NO_DOCTOR_AVAILABLE(
            HttpStatus.UNPROCESSABLE_ENTITY,
            "No doctor of that speciality is available at the requested time."),
    NO_ROOM_AVAILABLE(HttpStatus.UNPROCESSABLE_ENTITY, "No room is available at the requested time."),
    SLOT_TAKEN(HttpStatus.CONFLICT, "Another booking took the slot first. Retrying may succeed."),
    TEMPORARILY_UNAVAILABLE(
            HttpStatus.CONFLICT,
            "A doctor and a room are free, but another booking is holding them right now. Retrying may succeed."),
    PATIENT_ALREADY_BOOKED(HttpStatus.CONFLICT, "That patient already has an appointment at that time.");

    private final HttpStatus status;
    private final String detail;

    BookingError(HttpStatus status, String detail) {
        this.status = status;
        this.detail = detail;
    }

    public HttpStatus status() {
        return status;
    }

    public String detail() {
        return detail;
    }
}
