package com.exercise.integration.email;

import static org.assertj.core.api.Assertions.assertThat;

import com.exercise.config.BookingProperties;
import com.exercise.config.NotificationProperties;
import com.exercise.messaging.AppointmentCreatedEvent;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.MailSender;

class SmtpNotificationSenderTest {

    private final RecordingMailSender mail = new RecordingMailSender();

    private final SmtpNotificationSender sender = new SmtpNotificationSender(
            mail,
            new BookingProperties(Duration.ofMinutes(30), ZoneId.of("Europe/Lisbon")),
            new NotificationProperties("appointments@clinic.example.com"));

    private static AppointmentCreatedEvent event(Instant startTime) {
        return new AppointmentCreatedEvent(
                UUID.randomUUID(),
                AppointmentCreatedEvent.TYPE,
                Instant.parse("2026-09-10T08:00:00Z"),
                1,
                42L,
                startTime,
                new AppointmentCreatedEvent.SpecialitySummary(2L, "Cardiologia"),
                new AppointmentCreatedEvent.DoctorSummary(7L, "Luis Teixeira", "luis@example.com"),
                new AppointmentCreatedEvent.PatientSummary(1L, "Miguel Sousa", "miguel@example.com"),
                new AppointmentCreatedEvent.RoomSummary(4L, "Sala 4"));
    }

    @Test
    void writesToThePatientWithTheDoctorAndTheRoom() {
        sender.sendBookingConfirmation(event(Instant.parse("2026-09-15T13:30:00Z")));

        assertThat(mail.sent.getFrom()).isEqualTo("appointments@clinic.example.com");
        assertThat(mail.sent.getTo()).containsExactly("miguel@example.com");
        assertThat(mail.sent.getSubject()).isEqualTo("Consulta confirmada");
        assertThat(mail.sent.getText())
                .contains("Miguel Sousa")
                .contains("Luis Teixeira")
                .contains("Sala 4")
                .contains("Cardiologia");
    }

    @Test
    void showsTheTimeInTheClinicZoneRatherThanUtc() {
        sender.sendBookingConfirmation(event(Instant.parse("2026-09-15T13:30:00Z")));

        assertThat(mail.sent.getText()).contains("15/09/2026 às 14:30").doesNotContain("13:30");
    }

    private static final class RecordingMailSender implements MailSender {

        private SimpleMailMessage sent;

        @Override
        public void send(SimpleMailMessage message) {
            this.sent = message;
        }

        @Override
        public void send(SimpleMailMessage... messages) {
            this.sent = messages[messages.length - 1];
        }
    }
}
