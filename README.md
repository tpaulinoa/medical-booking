# Medical Booking

Spring Boot service to book medical appointments. The patient asks for a speciality and a time, and the service
assigns an available doctor and room. After the booking, it updates the doctor's calendar, reserves the room and
sends a confirmation email to the patient.

This is an MVP. Some ideas for a next version are in [docs/v2.md](docs/v2.md).

## Stack

- Java 17, Spring Boot 3.5
- PostgreSQL 17 and Flyway
- Kafka
- Mailpit (fake SMTP)
- JUnit 5 and Testcontainers
- Docker

## How to run it locally?

You need Docker installed. Then run:

```
docker compose up -d --build --wait
```

This starts Postgres, Kafka, Mailpit and the API on port `8080`, with some dummy data loaded (`dev` profile).

- Swagger UI: http://localhost:8080/swagger-ui.html
- Mailpit, to see the emails sent: http://localhost:8025
- Health: http://localhost:8080/actuator/health

If you prefer to run the API outside Docker (JDK 17 needed):

```
docker compose stop app
./gradlew bootRunDev
```

Database, Kafka and mail are configured by environment variables (`DATASOURCE_URL`, `KAFKA_BOOTSTRAP_SERVERS`,
`MAIL_HOST`, etc.), see `application.yml`.

A step by step to test the whole flow manually, with the dummy data available, is in
[docs/manual-testing.md](docs/manual-testing.md).

## API

Create an appointment:

```
curl -i -X POST http://localhost:8080/appointments \
  -H 'Content-Type: application/json' \
  -d '{"patientId": 7, "specialityId": 2, "startTime": "2027-03-02T10:00:00Z"}'
```

`startTime` must be in the future, on a 30-minute slot (`:00` or `:30`) and inside the working hours of a doctor
of that speciality. Times are in UTC.

List appointments, newest first (`size` up to 50):

```
curl 'http://localhost:8080/appointments?page=0&size=30'
```

Errors return `{"code": "...", "detail": "..."}`:

| Status | Code | When |
|---|---|---|
| 400 | `INVALID_REQUEST`, `START_TIME_IN_THE_PAST`, `START_TIME_NOT_ON_GRID` | invalid input |
| 422 | `PATIENT_NOT_FOUND`, `SPECIALITY_NOT_FOUND` | unknown patient or speciality |
| 422 | `NO_DOCTOR_AVAILABLE`, `NO_ROOM_AVAILABLE` | nothing free at that time |
| 409 | `PATIENT_ALREADY_BOOKED` | the patient already has an appointment at that time |
| 409 | `SLOT_TAKEN`, `TEMPORARILY_UNAVAILABLE` | concurrent booking, can be retried (`Retry-After: 1`) |

## How it works

The booking runs in a single transaction: it locks the patient, picks the doctor of the speciality with the fewest
appointments that day, picks a free room and saves the appointment together with an outbox event.

Overbooking is prevented by unique constraints in the database (doctor and time, room and time, patient and
time). Doctor and room are selected with `FOR UPDATE SKIP LOCKED`, so concurrent requests get different ones
instead of failing.

The outbox is published to Kafka, and three consumers handle the email, the doctor's calendar and the room
reservation. The consumers are idempotent. The calendar and room systems are fake implementations that only log.

The event format, for other services that want to consume it, is in [docs/events.md](docs/events.md).

The diagrams are in [docs/diagrams.md](docs/diagrams.md).

## Assumptions

- This service is the source of truth for doctor and room availability. The external calendar and room system are
  only updated after the booking.
- Appointments are fixed 30-minute slots.
- Patients, doctors, rooms and specialities already exist (seed data), so the request uses a `patientId`.
- Any room fits any appointment.
- Authentication, cancellation and filters in the listing are out of scope.

## Tests

```
./gradlew build
```

Docker must be running, since the tests use Testcontainers for Postgres and Kafka. The build fails with less than
70% coverage.

## Project structure

```
com.exercise
├── appointment      booking and listing
│   └── api          controller and DTOs
├── clinic           patients, doctors, rooms, specialities
├── outbox           outbox table, publisher and cleanup
├── messaging        Kafka topic, event and consumers
├── integration      email, calendar and room reservation
├── config
└── common           error handling
```
