package com.exercise;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.kafka.KafkaContainer;

/**
 * One broker per Spring context, so two cached contexts never share a consumer group. The listeners use an empty host
 * because Kafka 3.9 in combined mode rejects 0.0.0.0.
 */
@TestConfiguration(proxyBeanMethods = false)
public class KafkaBrokerContainer {

    @Bean
    @ServiceConnection
    KafkaContainer kafkaBroker() {
        return new KafkaContainer("apache/kafka:3.9.0")
                .withEnv("KAFKA_LISTENERS", "PLAINTEXT://:9092,BROKER://:9093,CONTROLLER://:9094");
    }
}
