package com.exercise.config;

import jakarta.annotation.PreDestroy;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.DescribeClusterResult;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.stereotype.Component;

/** Spring Boot has no health indicator for Kafka, so this one asks the broker to describe the cluster. */
@Component("kafka")
class KafkaHealthIndicator implements HealthIndicator {

    private static final Duration TIMEOUT = Duration.ofSeconds(3);

    private final AdminClient client;

    KafkaHealthIndicator(KafkaAdmin admin) {
        this.client = AdminClient.create(admin.getConfigurationProperties());
    }

    @Override
    public Health health() {
        try {
            DescribeClusterResult cluster = client.describeCluster();
            String clusterId = cluster.clusterId().get(TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
            int brokers = cluster.nodes().get(TIMEOUT.toMillis(), TimeUnit.MILLISECONDS).size();

            return Health.up().withDetail("clusterId", clusterId).withDetail("brokers", brokers).build();
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            return Health.down(interrupted).build();
        } catch (Exception unreachable) {
            return Health.down(unreachable).build();
        }
    }

    @PreDestroy
    void close() {
        client.close(TIMEOUT);
    }
}
