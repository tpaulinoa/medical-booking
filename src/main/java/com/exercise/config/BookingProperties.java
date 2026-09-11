package com.exercise.config;

import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import java.time.ZoneId;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "booking")
public record BookingProperties(@NotNull Duration slotDuration, @NotNull ZoneId clinicZone) {
}
