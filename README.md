# Event Booking API

A production-grade RESTful API built with **Spring Boot 3.x** for creating events and managing seat bookings.

---

## Technology Stack

| Layer | Technology |
|---|---|
| Runtime | Java 17 |
| Framework | Spring Boot 3.2 |
| Build tool | Maven |
| Database | PostgreSQL 15+ |
| Migrations | Flyway |
| Cache / Idempotency | Redis 7+ |
| ORM | Spring Data JPA / Hibernate |
| Validation | Jakarta Bean Validation |
| API Docs | Springdoc OpenAPI 2 (Swagger UI) |

---

## Project Structure

```
event-api/
├── src/main/java/com/digicore/eventapi/
│   ├── EventApiApplication.java
│   ├── archive/                  # Soft-delete abstraction
│   │   ├── SoftDeletable.java    # Interface — marks entities as soft-deletable
│   │   └── ArchiveService.java   # stamps deletedAt, never issues physical DELETE
│   ├── config/
│   │   ├── RedisConfig.java      # StringRedisTemplate + ObjectMapper beans
│   │   └── SwaggerConfig.java    # OpenAPI metadata
│   ├── controllers/
│   │   ├── EventController.java
│   │   └── BookingController.java
│   ├── dto/
│   │   ├── request/
│   │   │   ├── CreateEventRequest.java
│   │   │   └── CreateBookingRequest.java
│   │   └── response/
│   │       ├── ApiResponse.java      # Uniform envelope for all responses
│   │       ├── EventResponse.java
│   │       └── BookingResponse.java
│   ├── exception/
│   │   ├── GlobalExceptionHandler.java
│   │   ├── ResourceNotFoundException.java
│   │   └── BusinessException.java
│   ├── models/
│   │   ├── enums/EventStatus.java
│   │   ├── Event.java             # @Version for optimistic locking
│   │   └── Booking.java
│   ├── repositories/
│   │   ├── EventRepository.java   # PESSIMISTIC_WRITE query for booking
│   │   └── BookingRepository.java
│   ├── services/
│   │   ├── EventService.java
│   │   └── BookingService.java
│   └── utils/
│       ├── IdempotencyService.java  # Redis-backed X-Idempotency-Key support
│       └── EventCacheService.java   # Redis cache for GET /events/{id}
└── src/main/resources/
    ├── application.yml
    └── db/migration/
        ├── V1__create_events_table.sql
        └── V2__create_bookings_table.sql
```

---

## Prerequisites

- Java 17+
- Maven 3.8+
- PostgreSQL 15 running on `localhost:5432`
- Redis 7 running on `localhost:6379`

---

## Build the Project

```bash
cd event-api
mvn clean package -DskipTests
```

---

## Database Setup

```sql
CREATE DATABASE eventdb;
-- Default credentials assumed: postgres / postgres
-- Update src/main/resources/application.yml if yours differ
```

Flyway runs migrations automatically on startup — no manual SQL needed.

---

## Run the Application

```bash
mvn spring-boot:run
```

Or with the packaged JAR:

```bash
java -jar target/event-api-1.0.0.jar
```

The server starts on **http://localhost:8080**.

---

## Access Swagger UI

Open your browser:

```
http://localhost:8080/swagger-ui.html
```

Raw OpenAPI spec:

```
http://localhost:8080/v3/api-docs
```

---

## API Reference

### Events

| Method | Path | Description |
|--------|------|-------------|
| `POST` | `/events` | Create a new event |
| `GET` | `/events` | List all events (paginated) |
| `GET` | `/events/{id}` | Get event by ID |

### Bookings

| Method | Path | Description |
|--------|------|-------------|
| `POST` | `/events/{id}/bookings` | Book a seat |
| `DELETE` | `/bookings/{id}` | Cancel a booking |
| `GET` | `/events/{id}/bookings` | List bookings for an event |

### Pagination Parameters (GET endpoints)

| Param | Default | Description |
|-------|---------|-------------|
| `page` | `0` | Zero-based page number |
| `size` | `10` | Items per page |
| `sortBy` | `eventDate` | Sort field |
| `direction` | `asc` | `asc` or `desc` |

---

## Idempotent Booking

To prevent duplicate submissions (network retries, double-clicks), include a unique key on booking requests:

```http
POST /events/{id}/bookings
X-Idempotency-Key: my-unique-request-id-abc123
Content-Type: application/json

{
  "attendeeName": "John Doe",
  "attendeeEmail": "john@example.com"
}
```

Sending the same key again within 24 hours returns the **original response** without re-processing.

---

## Business Rules

| Rule | Enforcement |
|------|-------------|
| Event date must be in the future | `@Future` on DTO + DB constraint |
| `totalSeats` must be > 0 | `@Min(1)` on DTO + DB check constraint |
| Cannot book a `CLOSED` event | Service layer check |
| Cannot book when `bookedSeats >= totalSeats` | Service layer check (under DB lock) |
| Duplicate email per event is rejected | `existsActiveBooking` query + partial unique index |
| Cancellation frees up a seat | Decrements `bookedSeats`, re-opens if `CLOSED` |
| Event auto-closes when fully booked | `autoCloseIfFull()` called after every booking |

---

## Concurrency & Idempotency Design

### Overbooking Prevention (two-layer strategy)

1. **Pessimistic write lock** — `SELECT ... FOR UPDATE` on the event row prevents two transactions from concurrently reading the same `bookedSeats` value and both deciding there is capacity.
2. **Optimistic lock** — `@Version` on `Event` provides a safety net: any stale write (e.g. bypassing JPA) causes an `ObjectOptimisticLockingFailureException`, returned as HTTP `409 Conflict`.

### Idempotency

Booking requests may include `X-Idempotency-Key`. The key maps to the serialised response in Redis with a 24-hour TTL. Duplicate requests short-circuit before hitting business logic.

---

## Soft Delete

Cancelled bookings and deleted events are **never physically removed**. Instead, a `deleted_at` timestamp is stamped via `ArchiveService`. All repository queries include a `WHERE deleted_at IS NULL` filter so archived records are invisible to normal operations but remain for audit/compliance.

---

## Running Tests

```bash
mvn test
```

Unit tests cover:
- `EventService` — create, get (cache hit/miss), list, auto-close
- `BookingService` — create, reject closed/full/duplicate, cancel, re-open on cancel

---

## Assumptions & Design Decisions

1. **Soft delete only** — no hard deletes anywhere; `deletedAt` is the single source of truth for archival state.
2. **Email normalisation** — emails are lower-cased and trimmed before storage and duplicate checks.
3. **Partial unique index** in PostgreSQL (`WHERE deleted_at IS NULL`) means a cancelled email can re-book the same event.
4. **Redis is optional for startup** — if Redis is unavailable, the application will fail fast at startup. For a more resilient setup, configure a fallback or make Redis optional with `@ConditionalOnBean`.
5. **No authentication** — out of scope per the assignment; production would add Spring Security with JWT.
6. **`bookedSeats` is denormalised** on the `events` row (rather than a live `COUNT(bookings)`) for O(1) capacity checks under lock.
