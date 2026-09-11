package com.exercise.clinic;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "patient")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Patient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sns_number", nullable = false, length = 9)
    private String snsNumber;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(name = "date_of_birth", nullable = false)
    private LocalDate dateOfBirth;

    @Column(nullable = false, length = 255)
    private String email;

    @Column(length = 30)
    private String phone;

    public Patient(String snsNumber, String name, LocalDate dateOfBirth, String email, String phone) {
        this.snsNumber = snsNumber;
        this.name = name;
        this.dateOfBirth = dateOfBirth;
        this.email = email;
        this.phone = phone;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Patient that)) {
            return false;
        }
        return snsNumber != null && snsNumber.equals(that.getSnsNumber());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(snsNumber);
    }
}
