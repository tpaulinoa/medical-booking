package com.exercise.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.exercise.AbstractKafkaTest;
import com.exercise.config.KafkaTopicProperties;
import com.exercise.outbox.EventPublisher;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.kafka.KafkaConnectionDetails;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;

class KafkaEventPublisherTest extends AbstractKafkaTest {

    @Autowired
    private EventPublisher publisher;

    @Autowired
    private KafkaTopicProperties topic;

    @Autowired
    private KafkaProperties kafkaProperties;

    @Autowired
    private KafkaConnectionDetails kafkaConnection;

    private Consumer<String, String> eavesdropper;

    @BeforeEach
    void listenToTheTopic() {
        Map<String, Object> config = kafkaProperties.buildConsumerProperties(null);
        config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaConnection.getBootstrapServers());
        config.put(ConsumerConfig.GROUP_ID_CONFIG, "test-" + UUID.randomUUID());
        config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        eavesdropper = new DefaultKafkaConsumerFactory<>(
                        config, new StringDeserializer(), new StringDeserializer())
                .createConsumer();
        eavesdropper.subscribe(List.of(topic.name()));
        eavesdropper.poll(Duration.ofSeconds(1));
    }

    @AfterEach
    void stopListening() {
        eavesdropper.close();
    }

    private ConsumerRecord<String, String> awaitOneRecord() {
        long deadline = System.currentTimeMillis() + 20_000;
        while (System.currentTimeMillis() < deadline) {
            ConsumerRecords<String, String> records = eavesdropper.poll(Duration.ofMillis(500));
            if (!records.isEmpty()) {
                return records.iterator().next();
            }
        }
        throw new AssertionError("no record arrived on " + topic.name());
    }

    private String header(ConsumerRecord<String, String> record, String name) {
        return new String(record.headers().lastHeader(name).value(), StandardCharsets.UTF_8);
    }

    @Test
    void sendsThePayloadUnchangedUnderTheAppointmentIdAsKey() {
        UUID eventId = UUID.randomUUID();
        String payload = "{\"appointment_id\":42,\"doctor\":{\"name\":\"Ana\"}}";

        publisher.publish(eventId, "42", "appointment_created", payload);

        ConsumerRecord<String, String> record = awaitOneRecord();
        assertThat(record.key()).isEqualTo("42");
        assertThat(record.value()).isEqualTo(payload);
    }

    @Test
    void carriesTheEventIdAndTypeAsHeaders() {
        UUID eventId = UUID.randomUUID();

        publisher.publish(eventId, "7", "appointment_created", "{}");

        ConsumerRecord<String, String> record = awaitOneRecord();
        assertThat(header(record, "event_id")).isEqualTo(eventId.toString());
        assertThat(header(record, "event_type")).isEqualTo("appointment_created");
    }

    /** An oversized record makes the producer refuse the send. */
    @Test
    void aRefusedSendThrows() {
        assertThatThrownBy(() ->
                        publisher.publish(UUID.randomUUID(), "1", "appointment_created", "x".repeat(2_000_000)))
                .isInstanceOf(RuntimeException.class);
    }
}
