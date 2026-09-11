package com.exercise.clinic;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "speciality")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Speciality {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 500)
    private String description;

    public Speciality(String name, String description) {
        this.name = name;
        this.description = description;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Speciality that)) {
            return false;
        }
        return name != null && name.equals(that.getName());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(name);
    }
}
