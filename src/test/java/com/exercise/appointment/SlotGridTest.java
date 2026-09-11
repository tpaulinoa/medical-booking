package com.exercise.appointment;

import static org.assertj.core.api.Assertions.assertThat;

import com.exercise.config.BookingProperties;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class SlotGridTest {

    private final SlotGrid grid =
            new SlotGrid(new BookingProperties(Duration.ofMinutes(30), ZoneId.of("Europe/Lisbon")));

    @ParameterizedTest
    @ValueSource(strings = {
        "2026-09-15T08:00:00Z",
        "2026-09-15T08:30:00Z",
        "2026-12-15T09:00:00Z",
        "2026-12-15T09:30:00Z"
    })
    void acceptsTimesThatFallOnTheGrid(String instant) {
        assertThat(grid.isOnGrid(Instant.parse(instant))).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "2026-09-15T08:15:00Z",
        "2026-09-15T08:01:00Z",
        "2026-09-15T08:00:30Z"
    })
    void rejectsTimesBetweenSlots(String instant) {
        assertThat(grid.isOnGrid(Instant.parse(instant))).isFalse();
    }

    @Test
    void dayBoundsFollowTheClinicZoneDuringSummerTime() {
        Instant summerAfternoon = Instant.parse("2026-09-15T13:30:00Z");

        assertThat(grid.startOfDay(summerAfternoon)).isEqualTo(Instant.parse("2026-09-14T23:00:00Z"));
        assertThat(grid.startOfNextDay(summerAfternoon)).isEqualTo(Instant.parse("2026-09-15T23:00:00Z"));
    }

    @Test
    void dayBoundsFollowTheClinicZoneDuringWinterTime() {
        Instant winterAfternoon = Instant.parse("2026-12-15T13:30:00Z");

        assertThat(grid.startOfDay(winterAfternoon)).isEqualTo(Instant.parse("2026-12-15T00:00:00Z"));
        assertThat(grid.startOfNextDay(winterAfternoon)).isEqualTo(Instant.parse("2026-12-16T00:00:00Z"));
    }

    @Test
    void aDayIsStillBoundedCorrectlyWhenTheClocksGoForward() {
        Instant duringTheSpringChangeover = Instant.parse("2026-03-29T12:00:00Z");

        assertThat(grid.startOfDay(duringTheSpringChangeover))
                .isEqualTo(Instant.parse("2026-03-29T00:00:00Z"));
        assertThat(grid.startOfNextDay(duringTheSpringChangeover))
                .isEqualTo(Instant.parse("2026-03-29T23:00:00Z"));
    }
}
