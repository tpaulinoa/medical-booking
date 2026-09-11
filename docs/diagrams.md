# Diagrams

1. [Booking an appointment](#1-booking-an-appointment)
2. [After the booking](#2-after-the-booking)
3. [Data model](#3-data-model)
4. [Listing appointments](#4-listing-appointments)

---

## 1. Booking an appointment

```mermaid
sequenceDiagram
    autonumber
    actor C as Client
    participant S as AppointmentService
    participant DB as PostgreSQL

    C->>S: POST /appointments (patientId, specialityId, startTime)

    break startTime in the past or off the 30-minute grid
        S-->>C: 400 START_TIME_IN_THE_PAST / START_TIME_NOT_ON_GRID
    end

    Note over S,DB: one transaction

    S->>DB: lock the patient row (waits for the patient's other bookings)
    break patient or speciality unknown
        S-->>C: 422 PATIENT_NOT_FOUND / SPECIALITY_NOT_FOUND
    end

    S->>DB: does the patient already have an appointment at startTime?
    break yes
        S-->>C: 409 PATIENT_ALREADY_BOOKED
    end

    S->>DB: free doctor of the speciality, fewest appointments that day,<br/>random tie-break, FOR UPDATE SKIP LOCKED LIMIT 1
    break none returned
        S->>DB: same query without the lock
        S-->>C: 422 NO_DOCTOR_AVAILABLE if none exists<br/>409 TEMPORARILY_UNAVAILABLE if all are locked
    end

    S->>DB: free room, FOR UPDATE SKIP LOCKED LIMIT 1
    break none returned
        S->>DB: same query without the lock
        S-->>C: 422 NO_ROOM_AVAILABLE if none exists<br/>409 TEMPORARILY_UNAVAILABLE if all are locked
    end

    S->>DB: INSERT appointment
    break a unique constraint fires
        S-->>C: 409 SLOT_TAKEN / PATIENT_ALREADY_BOOKED
    end
    S->>DB: INSERT outbox_event (appointment_created)

    Note over S,DB: commit

    S-->>C: 201 Created (doctor, room)
```

The unique constraints on `(doctor_id, start_time)`, `(room_id, start_time)` and `(patient_id, start_time)` are
what guarantee there is no overbooking, with any number of instances running. The locks only keep concurrent
requests from colliding: with `SKIP LOCKED`, a booking that finds a doctor or room held by another transaction
takes the next candidate instead of waiting. The patient row is the one lock that waits, so two requests from the
same patient are answered one after the other and the second always sees the first.

No external system is called inside the transaction.

---

## 2. After the booking

```mermaid
sequenceDiagram
    autonumber
    participant DB as PostgreSQL
    participant OP as OutboxPublisher
    participant K as Kafka (appointment-events)
    participant PN as patient-notification
    participant DC as doctor-calendar
    participant RR as room-reservation
    participant EXT as SMTP / calendar / room system

    loop every second
        OP->>DB: unpublished events, FOR UPDATE SKIP LOCKED
        OP->>K: send, keyed by appointment id, and wait for the ack
        OP->>DB: set published_at
    end

    par one consumer group per action
        K-->>PN: appointment_created
        PN->>DB: already processed by this consumer?
        PN->>EXT: send the confirmation email
        PN->>DB: record (event_id, consumer)
    and
        K-->>DC: appointment_created
        DC->>EXT: add the appointment to the doctor's calendar
    and
        K-->>RR: appointment_created
        RR->>EXT: reserve the room
    end

    Note over DC,RR: same check → act → record as patient-notification
```

The publisher sends first and marks second, so an event can be sent twice but never lost. Each consumer checks
`processed_event` before acting and records the event after, which absorbs the duplicates. A failing action is
tried four times, with exponential backoff starting at one second; after that it is logged and the consumer moves on.
Published outbox rows are deleted after 30 days.

The email goes out over SMTP (Mailpit, locally). The calendar and room-reservation adapters are fakes that log.

---

## 3. Data model

```mermaid
erDiagram
    PATIENT ||--o{ APPOINTMENT : books
    DOCTOR ||--o{ APPOINTMENT : attends
    ROOM ||--o{ APPOINTMENT : hosts
    SPECIALITY ||--o{ APPOINTMENT : "requested for"
    DOCTOR ||--o{ DOCTOR_SPECIALITY : practises
    SPECIALITY ||--o{ DOCTOR_SPECIALITY : "practised by"
    DOCTOR ||--o{ DOCTOR_WORKING_DAY : "works on"

    APPOINTMENT {
        bigint id PK
        bigint patient_id FK
        bigint doctor_id FK
        bigint room_id FK
        bigint speciality_id FK
        timestamptz start_time
        timestamptz created_at
        timestamptz updated_at
    }
    PATIENT {
        bigint id PK
        varchar sns_number UK
        varchar name
        date date_of_birth
        varchar email UK
        varchar phone
    }
    DOCTOR {
        bigint id PK
        varchar name
        varchar email UK
        time working_hours_start
        time working_hours_end
    }
    DOCTOR_WORKING_DAY {
        bigint doctor_id PK, FK
        varchar day_of_week PK
    }
    DOCTOR_SPECIALITY {
        bigint doctor_id PK, FK
        bigint speciality_id PK, FK
    }
    SPECIALITY {
        bigint id PK
        varchar name UK
        varchar description
    }
    ROOM {
        bigint id PK
        varchar room_number UK
    }
    OUTBOX_EVENT {
        uuid id PK
        varchar domain_key
        varchar event_type
        jsonb payload
        timestamptz created_at
        timestamptz published_at
    }
    PROCESSED_EVENT {
        uuid event_id PK
        varchar consumer_name PK
        timestamptz processed_at
    }
```

Constraints on `appointment`:

- `UNIQUE (doctor_id, start_time)`: a doctor is never booked twice at the same time.
- `UNIQUE (room_id, start_time)`: same for rooms.
- `UNIQUE (patient_id, start_time)`: same for patients, which also stops a double submit from creating two bookings.

All three compare `start_time` for equality. With a fixed 30-minute grid, equal start times are the only way two
appointments can overlap.

`outbox_event` and `processed_event` have no foreign keys to the core tables: they are messaging infrastructure and
can be dropped or replaced without touching the domain schema.

---

## 4. Listing appointments

```mermaid
sequenceDiagram
    autonumber
    actor A as Admin
    participant C as AppointmentController
    participant S as AppointmentService
    participant DB as PostgreSQL

    A->>C: GET /appointments?page&size
    break page below 0, or size outside 1 to 50
        C-->>A: 400 INVALID_REQUEST
    end
    C->>S: list(page, size)
    S->>DB: appointments with patient, doctor, room and speciality,<br/>ORDER BY id DESC LIMIT size OFFSET page × size
    S->>DB: count(*)
    S-->>C: one page
    C-->>A: 200 OK (content + page metadata)
```

Newest bookings come first. There are no filters.
