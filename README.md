# Gym CRM — REST API

A REST API for a gym CRM: trainee and trainer profiles, the trainees' trainers, trainings and training types.
Built with plain Spring (no Spring Boot): Spring MVC runs in an embedded Tomcat, and data is stored in
PostgreSQL through Hibernate.

## Tech stack

- Java 17 or newer
- Spring Framework 7 (`spring-webmvc`, `spring-orm`), configured with annotations
- Embedded Tomcat 11
- Hibernate ORM 7 (JPA), HikariCP, PostgreSQL
- Jackson 3 for JSON, Hibernate Validator for request validation
- Swagger (OpenAPI 3) annotations for API documentation
- SLF4J + Logback, Lombok
- JUnit 5, Mockito, Spring MockMvc, H2 (tests only)
- Gradle

## Running

### 1. Database

Create a PostgreSQL user and database (the defaults in `application.properties`):

```bash
psql -U postgres -c "CREATE USER gymcrm WITH PASSWORD 'gymcrm';"
psql -U postgres -c "CREATE DATABASE gymcrm OWNER gymcrm;"
```

The tables are created on start-up by `db/schema.sql`, and the training types are inserted by
`db/data.sql`. Both scripts can run on every start. Hibernate only validates that the tables match the
entities (`hibernate.hbm2ddl.auto=validate`).

The database user and password can be overridden with the `DB_USERNAME` and `DB_PASSWORD` environment variables.

### 2. Server

Run `io.gymcrm.Application`. It starts Tomcat on port 8080 (but it can be configured)
and logs `Gym CRM started on http://localhost:8080`. If the database can't be reached, the application
exits with an error instead of starting.

### 3. You can try it

```bash
# Register a trainer (no authentication); the response contains the generated username and password
curl -i -H "Content-Type: application/json" \
     -d '{"firstName":"Anna","lastName":"Lee","specializationId":6}' \
     localhost:8080/api/trainers

# Use them for everything else
curl -i -u Anna.Lee:<password> localhost:8080/api/trainers/Anna.Lee
```

### Tests

```bash
./gradlew test
```

The tests use an in-memory H2 database, so no PostgreSQL is needed.

## Running in Docker

The image is built by the multi-stage `Dockerfile`: the first stage compiles the jar with Gradle inside a
JDK container, the second contains only a JRE and the jar and runs as a non-root user.

### 1. Without external systems (integrations disabled)

By default the container uses the `standalone` profile: an in-memory H2 database, so it needs nothing else.
Data is lost when the container stops.

```bash
docker build -t gym-crm:1.0 .
docker run -d --name gymcrm-standalone -p 8080:8080 gym-crm:1.0
docker ps                                   # STATUS shows "(healthy)" once /actuator/health answers
curl -i -H "Content-Type: application/json" \
     -d '{"firstName":"Anna","lastName":"Lee","specializationId":6}' localhost:8080/api/trainers
docker rm -f gymcrm-standalone
```

### 2. With PostgreSQL on a Docker network (integrations enabled)

The application and the database run in separate containers on a user-defined network, where they find each
other by container name. The database port isn't published, so only containers on the network can reach it.

With Docker Compose (`docker-compose.yml`):

```bash
docker compose up -d --build                # network gymcrm-net, containers gymcrm-db and gymcrm-app
docker compose ps
docker compose down                         # add -v to also delete the database volume
```

The same with plain `docker` commands:

```bash
docker network create gymcrm-net
docker run -d --name gymcrm-db --network gymcrm-net \
     -e POSTGRES_DB=gymcrm -e POSTGRES_USER=gymcrm -e POSTGRES_PASSWORD=gymcrm \
     -v gymcrm-data:/var/lib/postgresql/data postgres:17-alpine
docker run -d --name gymcrm-app --network gymcrm-net -p 8080:8080 \
     -e SPRING_PROFILES_ACTIVE=local -e DB_HOST=gymcrm-db -e DB_USERNAME=gymcrm -e DB_PASSWORD=gymcrm \
     gym-crm:1.0
docker network inspect gymcrm-net           # both containers and their IP addresses
```

The application logs which database it uses: `Environment [local]: database jdbc:postgresql://gymcrm-db:5432/gymcrm`.

### 3. Logs and a shell inside the container

```bash
docker logs -f gymcrm-app                   # the console output
docker exec -it gymcrm-app sh               # a shell inside the running container
tail -f /app/logs/gym-crm.log               #   (inside) the same logs, written to a file
docker exec -it gymcrm-db psql -U gymcrm -d gymcrm -c "select username, is_active from users"
```

Every log line contains the request's transaction id, which is also returned in the `X-Transaction-Id` header.

## API

| #  | Operation                          | Method and URL                                     | Success |
|----|------------------------------------|----------------------------------------------------|---------|
| 1  | Trainee registration               | `POST /api/trainees`                               | 201     |
| 2  | Trainer registration               | `POST /api/trainers`                               | 201     |
| 3  | Login                              | `GET /api/auth/login`                              | 200     |
| 4  | Change password                    | `PUT /api/auth/password`                           | 200     |
| 5  | Get trainee profile                | `GET /api/trainees/{username}`                     | 200     |
| 6  | Update trainee profile             | `PUT /api/trainees/{username}`                     | 200     |
| 7  | Delete trainee profile             | `DELETE /api/trainees/{username}`                  | 200     |
| 8  | Get trainer profile                | `GET /api/trainers/{username}`                     | 200     |
| 9  | Update trainer profile             | `PUT /api/trainers/{username}`                     | 200     |
| 10 | Active trainers not assigned       | `GET /api/trainees/{username}/available-trainers`  | 200     |
| 11 | Update trainee's trainers list     | `PUT /api/trainees/{username}/trainers`            | 200     |
| 12 | Trainee trainings (filters)        | `GET /api/trainees/{username}/trainings`           | 200     |
| 13 | Trainer trainings (filters)        | `GET /api/trainers/{username}/trainings`           | 200     |
| 14 | Add training                       | `POST /api/trainings`                              | 200     |
| 15 | Activate/de-activate trainee       | `PATCH /api/trainees/{username}/status`            | 200     |
| 16 | Activate/de-activate trainer       | `PATCH /api/trainers/{username}/status`            | 200     |
| 17 | Training types                     | `GET /api/training-types`                          | 200     |

The request and response fields, and every possible error, are documented with Swagger annotations in the
controllers (`io.gymcrm.web.controller`).

Example: a trainee profile (5, 6)

```json
{
  "username": "John.Smith",
  "firstName": "John",
  "lastName": "Smith",
  "dateOfBirth": "1995-04-12",
  "address": "Tashkent",
  "isActive": true,
  "trainers": [
    { "username": "Anna.Lee", "firstName": "Anna", "lastName": "Lee",
      "specialization": { "id": 6, "trainingType": "Yoga" } }
  ]
}
```

Training filters are query parameters, all optional: `from`, `to` (dates as `yyyy-MM-dd`), `trainerName` or
`traineeName` (part of the first or last name, case-insensitive) and `trainingType` (type name).
Example: `GET /api/trainees/John.Smith/trainings?from=2026-09-01&trainerName=anna`.

### Rules

- **Authentication.** Every endpoint except the two registrations and the password change needs an HTTP Basic
  `Authorization` header (`curl -u username:password`). The password change is authenticated by the old
  password in its body. The check happens in the service layer, so it can't be skipped by calling a service
  directly.
- **Own profile only.** Changing a profile (update, delete, status, trainers list) is only allowed on the
  authenticated user's own profile; otherwise the response is `403`. Any authenticated user can read profiles
  and training lists. A training can be added by its trainee or its trainer.
- **Usernames** are generated at registration as `firstName.lastName`, with the lowest free number added if
  the name is taken (`John.Smith1`). They can't be changed.
- **Passwords** are random 10-character strings, returned only once, in the registration response.
- **Activate/de-activate is not idempotent.** Requesting the state the user is already in returns `409`.
- **A trainer's specialization is read-only.** A `specializationId` sent in an update is ignored.
- **A new training** gets the trainer's specialization as its type, and the trainer is added to the trainee's
  trainers list. Trainings can't be changed or deleted through the API.
- **Deleting a trainee** also deletes their trainings.

### Errors


```json
{ "title": "Not Found", "status": 404, "detail": "Trainee not found: Nobody", "instance": "/api/trainees/Nobody" }
```

| Status | When                                                                          |
|--------|-------------------------------------------------------------------------------|
| 400    | Invalid input; validation errors list every invalid field under `"errors"`    |
| 401    | Missing or wrong credentials, or the wrong role (e.g. a trainer calling a trainee operation) |
| 403    | Changing someone else's profile, or adding someone else's training            |
| 404    | Unknown user, trainer or training type, or unknown URL                        |
| 409    | Activating an active user or de-activating an inactive one                    |
| 500    | Unexpected error; the details are logged, not returned                        |

## Logging

There are two levels of logging:

1. **Transaction level.** `TransactionIdFilter` gives every request a transaction id, or reuses a valid one
   sent by the caller in the `X-Transaction-Id` header. The id is printed in every log line of the request,
   from the controllers down to the DAOs, and returned in the `X-Transaction-Id` response header.
   `TransactionId.current()` returns it, to pass on to downstream services.
2. **REST call details.** `RestCallLoggingFilter` logs each call when it arrives and when it completes: the
   endpoint, the request body, the status, the duration and the response body. Passwords are masked (`***`),
   and the `Authorization` header is never logged.

```
INFO  [5a02a33e-…] RestCallLoggingFilter - Request POST /api/trainers
INFO  [5a02a33e-…] TrainerServiceImpl    - Trainer created: Anna.Lee (id 1), specialization Yoga
INFO  [5a02a33e-…] RestCallLoggingFilter - Response POST /api/trainers 201 in 332 ms, request body: {...}, response body: {"username":"Anna.Lee","password":"***"}
```

## Project structure

```
io.gymcrm
├── Application                  starts embedded Tomcat with the DispatcherServlet and the filters
├── config                       AppConfig (core), PersistenceConfig (DataSource, Hibernate, transactions)
├── entities                     User, Trainee, Trainer, Training, TrainingType (JPA)
├── dto                          facade inputs: Credentials, registrations, updates, training criteria
├── exceptions                   AuthenticationException (401), ForbiddenException (403),
│                                NotFoundException (404), ConflictException (409)
├── dao                          DAOs using the EntityManager and the Criteria API
├── services                     business logic, validation, authentication and "own profile only"
│                                checks, @Transactional
├── facade                       GymFacade: single entry point, delegates to the services
├── util                         UsernameGenerator, PasswordGenerator
└── web
    ├── config                   WebConfig (Spring MVC), OpenApiConfig (API-wide Swagger definitions)
    ├── auth                     reads the Basic Authorization header into Credentials
    ├── controller               the REST controllers
    ├── dto                      request records (with validation) and response records
    ├── mapper                   WebMapper: requests → facade inputs, entities → responses
    ├── error                    GlobalExceptionHandler: exceptions → status codes and problem details
    └── logging                  TransactionIdFilter, RestCallLoggingFilter
```

Controllers never return entities: responses are built by `WebMapper`, so the JSON doesn't depend on the
entity classes, and fields such as passwords can't leak by accident.

## Tests

- **Unit tests** (Mockito) for the services, the facade, the generators, the Basic auth parsing, the mapper,
  the request validation and the logging filters.
- **Controller unit tests** (`web.controller.unit`) test each controller with a mocked facade, for successful
  and failing requests.
- **DAO tests** run every query against H2.
- **`ServiceAuthenticationIntegrationTest`** calls the services directly and shows that every operation
  rejects wrong credentials and other users' profiles without changing the database.
- **Integration tests** send HTTP requests through the whole application against H2:
  `RestApiIntegrationTest` (successful paths), `ErrorHandlingIntegrationTest` (every error status) and
  `LoggingIntegrationTest` (one transaction id across all log lines of a request).
- **`ApiDocumentationTest`** checks that the Swagger annotations are complete and agree with the code.