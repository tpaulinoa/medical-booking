package com.exercise.integration.calendar;

import com.exercise.messaging.AppointmentCreatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Fake implementation that only logs. A real one would send the appointment id, so a repeated call does not create a
 * second entry.
 */
@Component
class LoggingCalendarClient implements CalendarClient {

    private static final Logger log = LoggerFactory.getLogger(LoggingCalendarClient.class);

    @Override
    public void addAppointment(AppointmentCreatedEvent appointment) {
        log.info(
                "calendar: doctor={} slot={} correlation={}",
                appointment.doctor().id(),
                appointment.startTime(),
                appointment.appointmentId());
    }
}
