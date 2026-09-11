# Events

After a booking, the service publishes an event to Kafka, so other services can react to it. This is the
contract for anyone who wants to consume it.

## Topic

| | |
|---|---|
| Topic | `appointment-events` |
| Key | the appointment id, as text. All events of one appointment go to the same partition, in order. |
| Headers | `event_id` (a UUID) and `event_type` |
| Value | JSON, UTF-8 |
| Retention | 7 days |

A new consumer should use its own consumer group. The ones used by this service are `patient-notification`,
`doctor-calendar` and `room-reservation`.

## appointment_created

Published once an appointment is booked:

```json
{
  "event_id": "b24a4f05-02ea-40af-a90e-241235f2a7ee",
  "event_type": "appointment_created",
  "occurred_at": "2026-09-11T00:58:32.050Z",
  "version": 1,
  "appointment_id": 6,
  "start_time": "2027-03-02T10:00:00Z",
  "speciality": { "id": 2, "name": "Cardiologia" },
  "doctor": { "id": 10, "name": "Luis Teixeira", "email": "luis.teixeira@example.com" },
  "patient": { "id": 7, "name": "Andre Coelho", "email": "andre.coelho@example.com" },
  "room": { "id": 1, "number": "Sala 1" }
}
```

- Times are in UTC.
- `speciality` is the one requested in the booking, since a doctor can have more than one.
- The event has everything needed to act on it, so a consumer does not need to call this service.
- `version` is the version of the payload, currently 1.

## Delivery

Delivery is at least once, so the same event can arrive more than once. A consumer should use `event_id` to skip
the events it already handled, like the consumers in this service do.
