package com.exercise.integration.roomreservation;

import com.exercise.messaging.AppointmentCreatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
class LoggingRoomReservationClient implements RoomReservationClient {

    private static final Logger log = LoggerFactory.getLogger(LoggingRoomReservationClient.class);

    @Override
    public void reserve(AppointmentCreatedEvent appointment) {
        log.info(
                "room reservation: room={} slot={} correlation={}",
                appointment.room().number(),
                appointment.startTime(),
                appointment.appointmentId());
    }
}
