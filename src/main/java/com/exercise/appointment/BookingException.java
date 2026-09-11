package com.exercise.appointment;

import java.io.Serial;

public class BookingException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    private final transient BookingError error;

    public BookingException(BookingError error) {
        super(error.detail());
        this.error = error;
    }

    public BookingError error() {
        return error;
    }
}
