package com.exercise.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.exercise.AbstractKafkaTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.actuate.health.Status;

class KafkaHealthIndicatorTest extends AbstractKafkaTest {

    @Autowired
    private HealthIndicator kafka;

    @Test
    void reportsUpAndNamesTheClusterWhenTheBrokerAnswers() {
        var health = kafka.health();

        assertThat(health.getStatus()).isEqualTo(Status.UP);
        assertThat(health.getDetails()).containsKey("clusterId");
        assertThat(health.getDetails()).containsEntry("brokers", 1);
    }
}
