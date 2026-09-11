package com.exercise.integration.calendar;

import com.exercise.messaging.AppointmentCreatedEvent;

/**
 * Adds the appointment to the doctor's calendar. The calendar is only informed; this service decides availability.
 */
public interface CalendarClient {

    void addAppointment(AppointmentCreatedEvent appointment);
}
