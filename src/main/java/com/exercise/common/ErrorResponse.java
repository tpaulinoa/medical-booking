package com.exercise.common;

import com.exercise.appointment.BookingError;

public record ErrorResponse(BookingError code, String detail) {}
