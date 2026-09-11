package com.exercise.messaging.consumer;

import com.exercise.integration.roomreservation.RoomReservationClient;
import com.exercise.messaging.IdempotentEventHandler;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
class RoomReservationConsumer {

    static final String GROUP = "room-reservation";

    private final IdempotentEventHandler handler;
    private final RoomReservationClient rooms;

    RoomReservationConsumer(IdempotentEventHandler handler, RoomReservationClient rooms) {
        this.handler = handler;
        this.rooms = rooms;
    }

    @KafkaListener(topics = "${kafka-topic.name}", groupId = GROUP)
    void onAppointmentCreated(ConsumerRecord<String, String> record) {
        handler.handle(record, GROUP, rooms::reserve);
    }
}
