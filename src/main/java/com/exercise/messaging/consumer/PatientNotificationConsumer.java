package com.exercise.messaging.consumer;

import com.exercise.integration.email.NotificationSender;
import com.exercise.messaging.IdempotentEventHandler;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
class PatientNotificationConsumer {

    static final String GROUP = "patient-notification";

    private final IdempotentEventHandler handler;
    private final NotificationSender notifications;

    PatientNotificationConsumer(IdempotentEventHandler handler, NotificationSender notifications) {
        this.handler = handler;
        this.notifications = notifications;
    }

    @KafkaListener(topics = "${kafka-topic.name}", groupId = GROUP)
    void onAppointmentCreated(ConsumerRecord<String, String> record) {
        handler.handle(record, GROUP, notifications::sendBookingConfirmation);
    }
}
