package com.exercise.integration.email;

import com.exercise.config.BookingProperties;
import com.exercise.config.NotificationProperties;
import com.exercise.messaging.AppointmentCreatedEvent;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.MailSender;
import org.springframework.stereotype.Component;

@Component
class SmtpNotificationSender implements NotificationSender {

    private static final Logger log = LoggerFactory.getLogger(SmtpNotificationSender.class);

    private static final DateTimeFormatter WHEN = DateTimeFormatter.ofPattern("dd/MM/yyyy 'às' HH:mm");

    private final MailSender mail;
    private final ZoneId clinicZone;
    private final String fromAddress;

    SmtpNotificationSender(MailSender mail, BookingProperties booking, NotificationProperties notification) {
        this.mail = mail;
        this.clinicZone = booking.clinicZone();
        this.fromAddress = notification.fromAddress();
    }

    @Override
    public void sendBookingConfirmation(AppointmentCreatedEvent appointment) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(appointment.patient().email());
        message.setSubject("Consulta confirmada");
        message.setText(body(appointment));

        mail.send(message);
        log.info("sent confirmation for appointment {}", appointment.appointmentId());
    }

    private String body(AppointmentCreatedEvent appointment) {
        String when = WHEN.format(appointment.startTime().atZone(clinicZone));

        return """
                Olá, %s.

                A sua consulta de %s está confirmada para %s.

                Profissional: %s
                Sala: %s

                Por favor, chegue com dez minutos de antecedência.
                """
                .formatted(
                        appointment.patient().name(),
                        appointment.speciality().name(),
                        when,
                        appointment.doctor().name(),
                        appointment.room().number());
    }
}
