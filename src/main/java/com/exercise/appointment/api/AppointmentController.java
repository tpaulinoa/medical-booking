package com.exercise.appointment.api;

import com.exercise.appointment.Appointment;
import com.exercise.appointment.AppointmentService;
import com.exercise.appointment.api.dto.AppointmentResponse;
import com.exercise.appointment.api.dto.CreateAppointmentRequest;
import com.exercise.common.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.net.URI;
import org.springframework.data.web.PagedModel;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * The error responses are declared with {@code @ApiResponse} annotations for simplicity. Ideally they would live in a
 * single openapi.yaml, where every response of the API can be read in one place.
 */
@RestController
@RequestMapping(path = "/appointments", produces = MediaType.APPLICATION_JSON_VALUE)
class AppointmentController {

    private final AppointmentService appointments;

    AppointmentController(AppointmentService appointments) {
        this.appointments = appointments;
    }

    @PostMapping
    @Operation(
            summary = "Book an appointment",
            description = "Assigns a free doctor of the requested speciality and a free room at the requested time.")
    @ApiResponse(responseCode = "201", description = "Booked.")
    @ApiResponse(
            responseCode = "400",
            description = "INVALID_REQUEST, START_TIME_IN_THE_PAST or START_TIME_NOT_ON_GRID.",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(
            responseCode = "409",
            description = "SLOT_TAKEN or TEMPORARILY_UNAVAILABLE, both with Retry-After: 1. "
                    + "PATIENT_ALREADY_BOOKED, without it.",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(
            responseCode = "422",
            description = "PATIENT_NOT_FOUND, SPECIALITY_NOT_FOUND, NO_DOCTOR_AVAILABLE or NO_ROOM_AVAILABLE.",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    ResponseEntity<AppointmentResponse> create(@Valid @RequestBody CreateAppointmentRequest request) {
        Appointment booked =
                appointments.book(request.patientId(), request.specialityId(), request.startTime());

        return ResponseEntity.created(URI.create("/appointments/" + booked.getId()))
                .body(AppointmentResponse.from(booked));
    }

    @GetMapping
    @Operation(summary = "List appointments", description = "One page at a time, most recently booked first.")
    @ApiResponse(responseCode = "200", description = "A page of appointments.")
    @ApiResponse(
            responseCode = "400",
            description = "INVALID_REQUEST: page or size out of range, or not a number.",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    PagedModel<AppointmentResponse> list(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "30") @Min(1) @Max(50) int size) {
        return new PagedModel<>(appointments.list(page, size).map(AppointmentResponse::from));
    }
}
