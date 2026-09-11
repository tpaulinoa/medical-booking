package com.exercise.appointment.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.exercise.appointment.Appointment;
import com.exercise.appointment.AppointmentService;
import com.exercise.appointment.BookingError;
import com.exercise.appointment.BookingException;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.mockito.BDDMockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.json.JsonCompareMode;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AppointmentController.class)
class AppointmentControllerTest {

    private static final String PAYLOAD =
            """
            {"patientId": 1, "specialityId": 2, "startTime": "2027-02-02T10:00:00Z"}
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AppointmentService appointments;

    @Test
    void returnsCreatedWithTheBookingDetails() throws Exception {
        Appointment booked = BDDMockito.mock(Appointment.class, BDDMockito.RETURNS_DEEP_STUBS);
        when(booked.getId()).thenReturn(42L);
        when(booked.getStartTime()).thenReturn(Instant.parse("2027-02-02T10:00:00Z"));
        when(booked.getSpeciality().getName()).thenReturn("Cardiologia");
        when(booked.getDoctor().getId()).thenReturn(7L);
        when(booked.getDoctor().getName()).thenReturn("Bruno Carvalho");
        when(booked.getDoctor().getEmail()).thenReturn("bruno.carvalho@example.com");
        when(booked.getRoom().getId()).thenReturn(4L);
        when(booked.getRoom().getRoomNumber()).thenReturn("Sala 4");
        when(booked.getPatient().getId()).thenReturn(1L);
        when(booked.getPatient().getName()).thenReturn("Miguel Sousa");
        when(booked.getPatient().getEmail()).thenReturn("miguel.sousa@example.com");
        when(appointments.book(eq(1L), eq(2L), any())).thenReturn(booked);

        mockMvc.perform(post("/appointments").contentType(MediaType.APPLICATION_JSON).content(PAYLOAD))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/appointments/42"))
                .andExpect(jsonPath("$.doctor.name").value("Bruno Carvalho"))
                .andExpect(jsonPath("$.room.number").value("Sala 4"))
                .andExpect(jsonPath("$.startTime").value("2027-02-02T10:00:00Z"));
    }

    @Test
    void rejectsAPayloadWithMissingFields() throws Exception {
        mockMvc.perform(post("/appointments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"patientId\": 1}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(content()
                        .json(
                                """
                                {"code": "INVALID_REQUEST",
                                 "detail": "specialityId must not be null; startTime must not be null"}
                                """,
                                JsonCompareMode.STRICT));
    }

    @Test
    void rejectsAPageSizeAboveTheMaximum() throws Exception {
        mockMvc.perform(get("/appointments").param("size", "51"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(content()
                        .json(
                                """
                                {"code": "INVALID_REQUEST", "detail": "size must be less than or equal to 50"}
                                """,
                                JsonCompareMode.STRICT));
    }

    @Test
    void mapsAnUnavailableDoctorToUnprocessableContent() throws Exception {
        when(appointments.book(any(), any(), any()))
                .thenThrow(new BookingException(BookingError.NO_DOCTOR_AVAILABLE));

        mockMvc.perform(post("/appointments").contentType(MediaType.APPLICATION_JSON).content(PAYLOAD))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(content()
                        .json(
                                """
                                {"code": "NO_DOCTOR_AVAILABLE", "detail": "%s"}
                                """
                                        .formatted(BookingError.NO_DOCTOR_AVAILABLE.detail()),
                                JsonCompareMode.STRICT))
                .andExpect(header().doesNotExist("Retry-After"));
    }

    @Test
    void mapsContentionToConflictWithARetryHint() throws Exception {
        when(appointments.book(any(), any(), any()))
                .thenThrow(new BookingException(BookingError.TEMPORARILY_UNAVAILABLE));

        mockMvc.perform(post("/appointments").contentType(MediaType.APPLICATION_JSON).content(PAYLOAD))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("TEMPORARILY_UNAVAILABLE"))
                .andExpect(header().string("Retry-After", "1"));
    }

    @Test
    void mapsADoubleBookedPatientToConflictWithoutARetryHint() throws Exception {
        when(appointments.book(any(), any(), any()))
                .thenThrow(new BookingException(BookingError.PATIENT_ALREADY_BOOKED));

        mockMvc.perform(post("/appointments").contentType(MediaType.APPLICATION_JSON).content(PAYLOAD))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("PATIENT_ALREADY_BOOKED"))
                .andExpect(header().doesNotExist("Retry-After"));
    }
}
