package com.exercise.config;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "kafka-consumer")
public record KafkaConsumerProperties(
        @Positive int maxAttempts, @NotNull Duration initialBackoff, @Positive double backoffMultiplier) {}
