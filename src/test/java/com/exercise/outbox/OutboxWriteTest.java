package com.exercise.outbox;

import static org.assertj.core.api.Assertions.assertThat;

import com.exercise.AbstractRepositoryTest;
import com.exercise.FrozenClock;
import com.exercise.TestClinic;
import com.exercise.appointment.Appointment;
import com.exercise.appointment.AppointmentService;
import com.exercise.appointment.SlotGrid;
import com.exercise.clinic.DoctorRepository;
import com.exercise.clinic.PatientRepository;
import com.exercise.clinic.RoomRepository;
import com.exercise.clinic.Speciality;
import com.exercise.clinic.SpecialityRepository;
import com.exercise.config.BookingProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Import;

@Import({AppointmentService.class, SlotGrid.class, FrozenClock.class, OutboxWriter.class})
@ImportAutoConfiguration(JacksonAutoConfiguration.class)
@EnableConfigurationProperties(BookingProperties.class)
class OutboxWriteTest extends AbstractRepositoryTest {

    private static final Instant SLOT = Instant.parse("2027-01-05T10:00:00Z");

    @Autowired
    private AppointmentService service;

    @Autowired
    private DoctorRepository doctors;

    @Autowired
    private RoomRepository rooms;

    @Autowired
    private PatientRepository patients;

    @Autowired
    private SpecialityRepository specialities;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private ObjectMapper objectMapper;

    private TestClinic clinic;
    private Speciality cardiology;

    @BeforeEach
    void buildAClinic() {
        clinic = new TestClinic(doctors, rooms, patients, specialities);
        cardiology = clinic.speciality("Cardiologia");
        clinic.doctor(cardiology);
        clinic.room();
    }

    @SuppressWarnings("unchecked")
    private List<Object[]> outboxRows() {
        entityManager.flush();
        return entityManager
                .createNativeQuery("SELECT id, domain_key, event_type, payload, published_at"
                        + " FROM outbox_event ORDER BY created_at")
                .getResultList();
    }

    @Test
    void bookingWritesExactlyOneUnpublishedEvent() throws Exception {
        Appointment booked = service.book(clinic.patient().getId(), cardiology.getId(), SLOT);

        List<Object[]> rows = outboxRows();
        assertThat(rows).hasSize(1);

        Object[] row = rows.get(0);
        assertThat(row[1]).isEqualTo(String.valueOf(booked.getId()));
        assertThat(row[2]).isEqualTo("appointment_created");
        assertThat(row[4]).describedAs("nothing has published it yet").isNull();

        JsonNode payload = objectMapper.readTree(String.valueOf(row[3]));
        assertThat(payload.get("event_id").asText()).isEqualTo(String.valueOf(row[0]));
        assertThat(payload.get("event_type").asText()).isEqualTo("appointment_created");
        assertThat(payload.get("version").asInt()).isEqualTo(1);
        assertThat(payload.get("appointment_id").asLong()).isEqualTo(booked.getId());
        assertThat(payload.get("start_time").asText()).isEqualTo("2027-01-05T10:00:00Z");
    }

    @Test
    void thePayloadCarriesEverythingAConsumerNeeds() throws Exception {
        Appointment booked = service.book(clinic.patient().getId(), cardiology.getId(), SLOT);

        JsonNode payload = objectMapper.readTree(String.valueOf(outboxRows().get(0)[3]));

        assertThat(payload.get("doctor").get("name").asText())
                .isEqualTo(booked.getDoctor().getName());
        assertThat(payload.get("doctor").get("email").asText())
                .isEqualTo(booked.getDoctor().getEmail());
        assertThat(payload.get("patient").get("email").asText())
                .isEqualTo(booked.getPatient().getEmail());
        assertThat(payload.get("room").get("number").asText())
                .isEqualTo(booked.getRoom().getRoomNumber());
        assertThat(payload.get("speciality").get("name").asText()).isEqualTo(cardiology.getName());
    }

    @Test
    void aBookingThatFailsWritesNoEvent() {
        Long patient = clinic.patient().getId();
        service.book(patient, cardiology.getId(), SLOT);

        assertThat(outboxRows()).hasSize(1);

        try {
            service.book(patient, cardiology.getId(), SLOT);
        } catch (RuntimeException expected) {
            // the second booking loses to the patient constraint
        }

        assertThat(outboxRows()).hasSize(1);
    }
}
