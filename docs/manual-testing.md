# Manual testing

How to test the whole flow by hand: book an appointment, see the event on Kafka and the email in Mailpit, and try
the error cases.

## Start

```
docker compose up -d --build --wait
```

Or, to run the API on the host:

```
docker compose stop app
./gradlew bootRunDev
```

Both use the `dev` profile, which loads the demo data. To start again with an empty database, run
`./gradlew dbReset` and start the API again.

## Demo data

| Speciality id | Name | Doctors |
|---|---|---|
| 1 | Medicina Geral e Familiar | 6 |
| 2 | Cardiologia | 2 |
| 3 | Pediatria | 1 |
| 4 | Ginecologia-Obstetrícia | 1 |
| 5 | Ortopedia | 1 |
| 6 | Urologia | 1 |
| 7 | Oftalmologia | none, on purpose |
| 8 | Otorrinolaringologia | 1, works Tuesday to Saturday |
| 9 | Psiquiatria | 2 |
| 10 | Medicina Interna | 2 |

Doctors work Monday to Friday, 09:00 to 18:00 Lisbon time, except the one of speciality 8 and one doctor who works
08:00 to 14:00. Patients have ids 1 to 20 and rooms are Sala 1 to Sala 8.

`startTime` is in UTC, but the working hours are in Lisbon time. Lisbon is UTC+0 until 28 March 2027 and UTC+1
after that, so in the examples below (early March 2027) both are the same.

## Book an appointment

In a second terminal, watch the Kafka topic:

```
MSYS_NO_PATHCONV=1 docker compose exec kafka /opt/kafka/bin/kafka-console-consumer.sh --bootstrap-server localhost:9092 --topic appointment-events --property print.key=true --property print.headers=true
```

(`MSYS_NO_PATHCONV=1` is only needed in Git Bash, which would rewrite the `/opt/...` path. It does nothing
elsewhere.)

Book:

```
curl -i -X POST http://localhost:8080/appointments \
  -H 'Content-Type: application/json' \
  -d '{"patientId": 7, "specialityId": 2, "startTime": "2027-03-02T10:00:00Z"}'
```

You should get a `201` with the doctor and the room. Then:

- In about a second, the event shows up in the other terminal, with the appointment id as key.
- The email is in Mailpit (http://localhost:8025), with the subject "Consulta confirmada".
- The database has the outbox row marked as published, and one processed event per consumer:

```
docker compose exec postgres psql -U medical_booking_usr -d medical_booking_db -c "SELECT domain_key, event_type, published_at FROM outbox_event ORDER BY created_at DESC LIMIT 3;"
docker compose exec postgres psql -U medical_booking_usr -d medical_booking_db -c "SELECT consumer_name, processed_at FROM processed_event ORDER BY processed_at DESC LIMIT 3;"
```

## List appointments

```
curl 'http://localhost:8080/appointments?page=0&size=30'
curl -i 'http://localhost:8080/appointments?size=51'
curl -i 'http://localhost:8080/appointments?page=-1'
```

The first returns the newest bookings first. The other two return `400 INVALID_REQUEST`.

## Error cases

A small helper, to print the body and the status code of each request:

```
book() { curl -s -w ' %{http_code}\n' -X POST http://localhost:8080/appointments -H 'Content-Type: application/json' -d "$1"; }
```

Then run the cases below, after the booking above. The comment shows the expected result.

```
book '{"patientId": 7, "specialityId": 2, "startTime": "2027-03-02T10:00:00Z"}'    # 409 PATIENT_ALREADY_BOOKED, same request again
book '{"patientId": 1, "specialityId": 7, "startTime": "2027-03-02T10:00:00Z"}'    # 422 NO_DOCTOR_AVAILABLE, no doctor for speciality 7
book '{"patientId": 1, "specialityId": 2, "startTime": "2027-03-02T10:15:00Z"}'    # 400 START_TIME_NOT_ON_GRID
book '{"patientId": 1, "specialityId": 2, "startTime": "2020-01-07T10:00:00Z"}'    # 400 START_TIME_IN_THE_PAST
book '{"patientId": 1, "specialityId": 2, "startTime": "2027-03-02T06:00:00Z"}'    # 422 NO_DOCTOR_AVAILABLE, before working hours
book '{"patientId": 1, "specialityId": 8, "startTime": "2027-03-01T10:00:00Z"}'    # 422 NO_DOCTOR_AVAILABLE, a Monday
book '{"patientId": 1, "specialityId": 8, "startTime": "2027-03-06T10:00:00Z"}'    # 201, a Saturday
book '{"patientId": 999, "specialityId": 2, "startTime": "2027-03-02T10:00:00Z"}'  # 422 PATIENT_NOT_FOUND
book '{"patientId": 1, "specialityId": 999, "startTime": "2027-03-02T10:00:00Z"}'  # 422 SPECIALITY_NOT_FOUND
book '{"patientId": 1}'                                                             # 400 INVALID_REQUEST
book '{"patientId": 1, "specialityId": 2, "startTime": "not a date"}'              # 400 INVALID_REQUEST
```

To see the doctor selection, book cardiology twice on a day with no bookings. The second one goes to the other
cardiologist, since the first now has more appointments that day:

```
book '{"patientId": 2, "specialityId": 2, "startTime": "2027-03-03T10:00:00Z"}'
book '{"patientId": 3, "specialityId": 2, "startTime": "2027-03-03T11:00:00Z"}'
```

`SLOT_TAKEN` and `TEMPORARILY_UNAVAILABLE` only happen with concurrent requests, so they are hard to get by hand.
`ConcurrentBookingTest` covers them.

## Troubleshooting

- **Port 8080 already in use:** the app container and `bootRunDev` both use it. Run `docker compose stop app`.
- **Health is DOWN:** `docker compose ps` shows which dependency is not running.
