# Gym CRM

A REST API for a gym CRM: trainee and trainer profiles, the trainees' trainers, trainings and training types.
A Spring Boot application secured with Spring Security and JWT, storing its data in PostgreSQL through Hibernate.

## Tech stack

- Java 17 or newer, Gradle
- Spring Boot 4 (Spring Framework 7, Spring MVC, embedded Tomcat)
- Spring Security with JWT (OAuth2 resource server support), BCrypt password hashing
- Hibernate ORM 7 (JPA), HikariCP, PostgreSQL; H2 for tests and the `standalone` profile
- Spring Boot Actuator, Micrometer with Prometheus
- Jackson 3 for JSON, Hibernate Validator for request validation
- Swagger (OpenAPI 3) annotations for API documentation
- SLF4J + Logback, Lombok
- JUnit 5, Mockito, Spring MockMvc
- Docker

## Running

### 1. Database

Create a PostgreSQL user and database (the defaults of the `local` profile, in `application-local.yml`):

```bash
psql -U postgres -c "CREATE USER gymcrm WITH PASSWORD 'gymcrm';"
psql -U postgres -c "CREATE DATABASE gymcrm OWNER gymcrm;"
```

The tables are created on start-up by `db/schema.sql`, and the training types are inserted by
`db/data.sql`. Both scripts can run on every start. Hibernate only validates that the tables match the
entities (`spring.jpa.hibernate.ddl-auto: validate`).

### 2. Server

Run `io.gymcrm.Application`, or:

```bash
./gradlew bootRun
```

It starts on port 8080 (change it with `SERVER_PORT` or `--server.port=9090`) and logs
`Started Application in ... seconds`. If the database can't be reached or a required setting is missing, the
application exits with an error instead of starting.

### 3. Environments

Choose one with `SPRING_PROFILES_ACTIVE`; without it, `local` is used.

| Profile      | Database                                  | From the environment                         |
|--------------|-------------------------------------------|----------------------------------------------|
| `local`      | PostgreSQL on `localhost`, `gymcrm`       | nothing required                             |
| `standalone` | in-memory H2, data is lost on stop        | nothing required                             |
| `dev`        | `dev-db.gymcrm.internal`, `gymcrm_dev`    | `DB_PASSWORD`, `JWT_SECRET`                  |
| `stg`        | `stg-db.gymcrm.internal`, `gymcrm_stg`    | `DB_PASSWORD`, `JWT_SECRET`                  |
| `prod`       | `DB_HOST`, `gymcrm`                       | `DB_HOST`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET` |

`JWT_SECRET` must be at least 32 bytes long. `CORS_ALLOWED_ORIGINS` sets which websites may call the API.

### 4. Try it

```bash
# Register (no token needed): the response has the generated username and password, and a token
curl -i -H "Content-Type: application/json" \
     -d '{"firstName":"Anna","lastName":"Lee","specializationId":6}' \
     localhost:8080/api/trainers

# Or log in to get a new token
curl -i -H "Content-Type: application/json" \
     -d '{"username":"Anna.Lee","password":"<password>"}' \
     localhost:8080/api/auth/login

# Use the token for everything else
curl -i -H "Authorization: Bearer <token>" localhost:8080/api/trainers/Anna.Lee

# Log out: the token stops working
curl -i -X POST -H "Authorization: Bearer <token>" localhost:8080/api/auth/logout
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
| 3  | Login, returns a token             | `POST /api/auth/login`                             | 200     |
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
| 18 | Logout, revokes the token          | `POST /api/auth/logout`                            | 200     |

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

### Security

- **Authentication.** Log in (or register) to get a JWT and send it as `Authorization: Bearer <token>`. Only the
  two registrations and the login work without a token. Tokens are valid for 1 hour and are signed with
  HMAC-SHA256, so they can't be changed or forged without the secret.
- **Passwords** are stored as salted BCrypt hashes. The generated password is returned only once, in the
  registration response.
- **Brute-force protection.** After 3 failed logins in a row, the username is blocked for 5 minutes, even with
  the right password (`429` with a `Retry-After` header). Unknown usernames are treated the same way.
- **Logout.** A token is valid on its own until it expires, so logout puts its id on a revocation list.
- **Authorization** is enforced in the service layer with `@PreAuthorize`, so it can't be skipped by calling a
  service directly. Changing a profile (update, delete, status, trainers list, password) is only allowed for
  its owner with the right role; otherwise the response is `403`. Any logged-in user can read profiles and
  training lists. A training can be added by its trainee or its trainer.
- **CORS.** Only the origins in `gymcrm.security.cors.allowed-origins` may call the API from a browser.
- **CSRF protection is off**, because it defends against cookies being sent automatically. This API uses no
  cookies or HTTP sessions: every request carries its token explicitly.

### Rules

- **Usernames** are generated at registration as `firstName.lastName`, with the lowest free number added if
  the name is taken (`John.Smith1`). They can't be changed.
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
| 401    | Wrong username or password; missing, invalid, expired or revoked token        |
| 403    | The wrong role, someone else's profile, or someone else's training            |
| 404    | Unknown user, trainer or training type, or unknown URL                        |
| 409    | Activating an active user or de-activating an inactive one                    |
| 429    | Login blocked after too many failed attempts; see `Retry-After`               |
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

## Monitoring

| Endpoint               | Token needed | Shows                                                               |
|------------------------|--------------|---------------------------------------------------------------------|
| `/actuator/health`     | no           | Every health check; details only on `local`, `dev` and `standalone` |
| `/actuator/info`       | no           | Application, build and Java information                             |
| `/actuator/prometheus` | no           | All metrics in Prometheus format                                    |
| `/actuator/metrics`    | yes          | All metrics as JSON                                                 |

Restrict the public endpoints at the network level on real servers.

- **Custom health checks:** `trainingTypes` (reference data present), `databaseResponseTime` (below a limit) and
  `trainerAvailability` (a custom `WARNING` status when a training type has no active trainer; still HTTP 200).
- **Custom metrics:** `gymcrm_registrations_total`, `gymcrm_logins_total` (success, failure, blocked),
  `gymcrm_authentication_failures_total`, `gymcrm_trainings_added_total`, `gymcrm_trainings_duration_minutes`
  and `gymcrm_users_active`.

## Project structure

```
io.gymcrm
├── Application                  the Spring Boot entry point
├── config                       RequiredSettingsCheck: stops the start-up if a required setting is missing
├── security                     SecurityConfig (filter chain, CORS, BCrypt), JwtConfig, JwtTokenService,
│                                TokenRevocationService, LoginAttemptService, GymUserDetailsService,
│                                JwtLogoutHandler, SecurityErrorHandler
├── entities                     User, Trainee, Trainer, Training, TrainingType (JPA)
├── dto                          facade inputs and results: Credentials, IssuedToken, CreatedProfile, ...
├── exceptions                   AuthenticationException (401), NotFoundException (404),
│                                ConflictException (409), LoginBlockedException (429)
├── dao                          DAOs using the EntityManager and the Criteria API
├── services                     business logic, validation, @PreAuthorize rules, @Transactional
├── facade                       GymFacade: single entry point, delegates to the services
├── health                       the custom health indicators
├── metrics                      GymMetrics, ActiveUsersMetrics
├── util                         UsernameGenerator, PasswordGenerator
└── web
    ├── config                   OpenApiConfig (API-wide Swagger definitions)
    ├── controller               the REST controllers
    ├── dto                      request records (with validation) and response records
    ├── mapper                   WebMapper: requests → facade inputs, entities → responses
    ├── error                    GlobalExceptionHandler: exceptions → status codes and problem details
    └── logging                  TransactionIdFilter, RestCallLoggingFilter
```

Controllers never return entities: responses are built by `WebMapper`, so the JSON doesn't depend on the
entity classes, and fields such as passwords can't leak by accident.

## Tests

- **Unit tests** (Mockito) for the services, the facade, the generators, the mapper, the request validation,
  the logging filters, the health indicators, the metrics, and the security services.
- **Controller unit tests** (`web.controller.unit`) test each controller with a mocked facade, for successful
  and failing requests.
- **DAO tests** run every query against H2.
- **`SecurityIntegrationTest`** attacks the application over HTTP: missing, tampered, expired, forged and revoked
  tokens, brute force, logout, CORS, and passwords or tokens in the logs.
- **`ServiceAuthenticationIntegrationTest`** calls the services directly and shows that they reject anonymous
  callers, the wrong role and other users' profiles without changing the database.
- **Integration tests** send HTTP requests through the whole application against H2:
  `RestApiIntegrationTest` (successful paths), `ErrorHandlingIntegrationTest` (every error status),
  `LoggingIntegrationTest`, `ActuatorIntegrationTest` and `PrometheusIntegrationTest`.
- **`ProfilesTest`** loads the real configuration of every environment.
- **`ApiDocumentationTest`** checks that the Swagger annotations are complete and agree with the code.

## Known limitations

- Failed-login counts and revoked tokens are kept in memory: they are lost on restart and not shared between
  instances. Running several instances would need a shared store such as Redis.
- Existing tokens stay valid after a password change, until they expire or the user logs out.
- Schema changes to an existing database aren't handled: `CREATE TABLE IF NOT EXISTS` skips existing tables. A
  migration tool such as Flyway would solve this.
- There is no Swagger UI yet; adding `springdoc-openapi-starter-webmvc-ui` would provide it.
