package com.exercise.appointment;

import com.exercise.config.BookingProperties;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import org.springframework.stereotype.Component;

@Component
public class SlotGrid {

    private final Duration slotDuration;
    private final ZoneId clinicZone;

    SlotGrid(BookingProperties properties) {
        this.slotDuration = properties.slotDuration();
        this.clinicZone = properties.clinicZone();
    }

    public boolean isOnGrid(Instant startTime) {
        if (startTime.getNano() != 0) {
            return false;
        }
        long secondOfLocalDay = startTime.atZone(clinicZone).toLocalTime().toSecondOfDay();
        return secondOfLocalDay % slotDuration.getSeconds() == 0;
    }

    public Instant startOfDay(Instant instant) {
        return instant.atZone(clinicZone).toLocalDate().atStartOfDay(clinicZone).toInstant();
    }

    public Instant startOfNextDay(Instant instant) {
        return instant.atZone(clinicZone).toLocalDate().plusDays(1).atStartOfDay(clinicZone).toInstant();
    }

    public DayOfWeek dayOfWeek(Instant instant) {
        return instant.atZone(clinicZone).getDayOfWeek();
    }

    public LocalTime localTime(Instant instant) {
        return instant.atZone(clinicZone).toLocalTime();
    }

    public LocalTime localEndTime(Instant instant) {
        return localTime(instant).plus(slotDuration);
    }

    public Duration slotDuration() {
        return slotDuration;
    }

    public ZoneId clinicZone() {
        return clinicZone;
    }
}
