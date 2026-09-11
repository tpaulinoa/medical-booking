package com.exercise.integration.roomreservation;

import com.exercise.messaging.AppointmentCreatedEvent;

/** Reserves the room in the external system. The system is only informed; this service decides availability. */
public interface RoomReservationClient {

    void reserve(AppointmentCreatedEvent appointment);
}
