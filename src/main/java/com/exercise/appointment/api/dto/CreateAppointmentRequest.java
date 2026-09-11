package com.exercise.appointment.api.dto;

import jakarta.validation.constraints.NotNull;
import java.time.Instant;

public record CreateAppointmentRequest(
        @NotNull Long patientId, @NotNull Long specialityId, @NotNull Instant startTime) {}
