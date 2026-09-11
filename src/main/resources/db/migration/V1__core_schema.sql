CREATE TABLE speciality (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name        VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    CONSTRAINT uq_speciality_name UNIQUE (name)
);

CREATE TABLE doctor (
    id                  BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name                VARCHAR(150) NOT NULL,
    email               VARCHAR(255) NOT NULL,
    working_hours_start TIME NOT NULL,
    working_hours_end   TIME NOT NULL,
    CONSTRAINT uq_doctor_email UNIQUE (email),
    CONSTRAINT ck_doctor_working_hours CHECK (working_hours_start < working_hours_end)
);

CREATE TABLE doctor_speciality (
    doctor_id     BIGINT NOT NULL REFERENCES doctor (id),
    speciality_id BIGINT NOT NULL REFERENCES speciality (id),
    PRIMARY KEY (doctor_id, speciality_id)
);

CREATE INDEX idx_doctor_speciality_speciality ON doctor_speciality (speciality_id);

CREATE TABLE doctor_working_day (
    doctor_id   BIGINT NOT NULL REFERENCES doctor (id),
    day_of_week VARCHAR(9) NOT NULL,
    PRIMARY KEY (doctor_id, day_of_week),
    CONSTRAINT ck_doctor_working_day CHECK (
        day_of_week IN ('MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY',
                        'FRIDAY', 'SATURDAY', 'SUNDAY')
    )
);

CREATE TABLE room (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    room_number VARCHAR(50) NOT NULL,
    CONSTRAINT uq_room_number UNIQUE (room_number)
);

CREATE TABLE patient (
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    sns_number    VARCHAR(9) NOT NULL,
    name          VARCHAR(150) NOT NULL,
    date_of_birth DATE NOT NULL,
    email         VARCHAR(255) NOT NULL,
    phone         VARCHAR(30),
    CONSTRAINT uq_patient_sns_number UNIQUE (sns_number),
    CONSTRAINT uq_patient_email UNIQUE (email),
    CONSTRAINT ck_patient_sns_number CHECK (sns_number ~ '^[0-9]{1,9}$')
);

CREATE TABLE appointment (
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    patient_id    BIGINT NOT NULL REFERENCES patient (id),
    doctor_id     BIGINT NOT NULL REFERENCES doctor (id),
    room_id       BIGINT NOT NULL REFERENCES room (id),
    speciality_id BIGINT NOT NULL REFERENCES speciality (id),
    start_time    TIMESTAMPTZ NOT NULL,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_appointment_doctor_slot  UNIQUE (doctor_id, start_time),
    CONSTRAINT uq_appointment_room_slot    UNIQUE (room_id, start_time),
    CONSTRAINT uq_appointment_patient_slot UNIQUE (patient_id, start_time)
);

CREATE INDEX idx_appointment_speciality ON appointment (speciality_id);
