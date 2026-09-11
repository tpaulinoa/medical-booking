package com.exercise.messaging.consumer;

import com.exercise.integration.calendar.CalendarClient;
import com.exercise.messaging.IdempotentEventHandler;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
class DoctorCalendarConsumer {

    static final String GROUP = "doctor-calendar";

    private final IdempotentEventHandler handler;
    private final CalendarClient calendar;

    DoctorCalendarConsumer(IdempotentEventHandler handler, CalendarClient calendar) {
        this.handler = handler;
        this.calendar = calendar;
    }

    @KafkaListener(topics = "${kafka-topic.name}", groupId = GROUP)
    void onAppointmentCreated(ConsumerRecord<String, String> record) {
        handler.handle(record, GROUP, calendar::addAppointment);
    }
}
