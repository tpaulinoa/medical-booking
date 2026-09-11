package com.exercise.clinic;

import static org.assertj.core.api.Assertions.assertThat;

import com.exercise.AbstractRepositoryTest;
import com.exercise.TestClinic;
import jakarta.persistence.EntityManager;
import org.hibernate.Hibernate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class EntityEqualityTest extends AbstractRepositoryTest {

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private DoctorRepository doctors;

    @Autowired
    private PatientRepository patients;

    @Autowired
    private RoomRepository rooms;

    @Autowired
    private SpecialityRepository specialities;

    private TestClinic clinic;

    @BeforeEach
    void buildTheClinic() {
        clinic = new TestClinic(doctors, rooms, patients, specialities);
    }

    /**
     * equals uses instanceof and getters because a Hibernate proxy is a subclass whose fields are empty until it is
     * loaded.
     */
    @Test
    void aLazyProxyEqualsTheEntityItStandsFor() {
        Long id = clinic.doctor(clinic.speciality("Cardiologia")).getId();

        entityManager.clear();
        Doctor loaded = doctors.findById(id).orElseThrow();

        entityManager.clear();
        Doctor proxy = doctors.getReferenceById(id);

        assertThat(Hibernate.isInitialized(proxy))
                .describedAs("the test is pointless unless this really is an uninitialised proxy")
                .isFalse();

        assertThat(loaded).isEqualTo(proxy);
        assertThat(proxy).isEqualTo(loaded);
    }
}
