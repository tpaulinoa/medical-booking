package com.exercise;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/**
 * Fixed clock, so the dates used in tests never end up in the past.
 *
 * <p>@Primary because the context already has the real clock.
 */
@TestConfiguration
public class FrozenClock {

    public static final Instant NOW = Instant.parse("2027-01-04T08:00:00Z");

    @Bean
    @Primary
    Clock frozenClock() {
        return Clock.fixed(NOW, ZoneOffset.UTC);
    }
}
