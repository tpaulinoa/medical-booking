package com.exercise.clinic;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "doctor")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Doctor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(nullable = false, length = 255)
    private String email;

    @Column(name = "working_hours_start", nullable = false)
    private LocalTime workingHoursStart;

    @Column(name = "working_hours_end", nullable = false)
    private LocalTime workingHoursEnd;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "doctor_working_day", joinColumns = @JoinColumn(name = "doctor_id"))
    @Column(name = "day_of_week", nullable = false, length = 9)
    @Enumerated(EnumType.STRING)
    private Set<DayOfWeek> workingDays = new HashSet<>();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "doctor_speciality",
            joinColumns = @JoinColumn(name = "doctor_id"),
            inverseJoinColumns = @JoinColumn(name = "speciality_id"))
    private Set<Speciality> specialities = new HashSet<>();

    public Doctor(String name, String email, LocalTime workingHoursStart, LocalTime workingHoursEnd) {
        this.name = name;
        this.email = email;
        this.workingHoursStart = workingHoursStart;
        this.workingHoursEnd = workingHoursEnd;
    }

    public void addSpeciality(Speciality speciality) {
        specialities.add(speciality);
    }

    public void addWorkingDay(DayOfWeek day) {
        workingDays.add(day);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Doctor that)) {
            return false;
        }
        return email != null && email.equals(that.getEmail());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(email);
    }
}
