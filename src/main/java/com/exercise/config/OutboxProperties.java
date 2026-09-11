package com.exercise.config;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "outbox")
public record OutboxProperties(
        @NotNull Duration pollInterval,
        @Positive int batchSize,
        @NotNull Duration retention,
        @NotNull Duration cleanupInterval) {}
