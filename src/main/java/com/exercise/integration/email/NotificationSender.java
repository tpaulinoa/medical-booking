package com.exercise.integration.email;

import com.exercise.messaging.AppointmentCreatedEvent;

public interface NotificationSender {

    void sendBookingConfirmation(AppointmentCreatedEvent appointment);
}
