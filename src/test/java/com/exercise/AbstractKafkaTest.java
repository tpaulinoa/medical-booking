package com.exercise;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.context.ImportTestcontainers;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

/** Starts the Kafka listeners, which are off in the other tests. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
@ImportTestcontainers(PostgresContainer.class)
@Import(KafkaBrokerContainer.class)
@TestPropertySource(properties = "spring.kafka.listener.auto-startup=true")
public abstract class AbstractKafkaTest {
}
