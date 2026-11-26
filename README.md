# Gym CRM

A REST API for a gym CRM: trainee and trainer profiles, the trainees' trainers, trainings and training types.
A Spring Boot application secured with Spring Security and JWT, storing its data in PostgreSQL through Hibernate.

The repository holds three services, each a Gradle project with its own Docker image, plus an ActiveMQ broker:

| Service                  | Folder              | Port | What it does                                                         |
|--------------------------|---------------------|------|----------------------------------------------------------------------|
| Gym CRM                  | `/` (root)          | 8080 | The main API. Reports every added or deleted training.               |
| Trainer Workload         | `trainer-workload/` | 8081 | Sums each trainer's training minutes per month (in-memory database). |
| Eureka Discovery Server  | `discovery-server/` | 8761 | The registry where the other two register and find each other.      |
| ActiveMQ Classic broker  | (Docker image)      | 61616, console 8161 | Carries added and deleted trainings to Trainer Workload. |

```
                 writes (asynchronous): an ADD or DELETE message per training, JSON + transactionId
 client ──► Gym CRM ──► ActiveMQ queue "trainer-workload.updates" ──► listener in Trainer Workload
               │                                       invalid messages ─► "trainer-workload.updates.DLQ"
               │
               │  reads (synchronous): where is "trainer-workload"? ──► Eureka ◄── registers ──┐
               └──► GET /api/trainer-workloads/{username}, service JWT, X-Transaction-Id ──► Trainer Workload
                    through a circuit breaker, with connect and read timeouts
```

## Tech stack

- Java 17 or newer, Gradle
- Spring Boot 4 (Spring Framework 7, Spring MVC, embedded Tomcat)
- Spring Security with JWT (OAuth2 resource server support), BCrypt password hashing
- Hibernate ORM 7 (JPA), HikariCP, PostgreSQL; H2 for tests and the `standalone` profile
- Spring Boot Actuator, Micrometer with Prometheus
- Spring Cloud: Netflix Eureka (discovery), Spring Cloud LoadBalancer, Circuit Breaker with Resilience4j
- Spring JMS with Apache ActiveMQ Classic 6 (asynchronous messaging, dead letter queue)
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
./gradlew test                     # all three services
./gradlew check                    # tests + line coverage of at least 80% (JaCoCo) per service
./gradlew :trainer-workload:test   # one service
```

The tests use an in-memory H2 database, an in-memory ActiveMQ broker and no Eureka, so nothing else needs to
run. Coverage reports are in
`build/reports/jacoco/test/html` of each service.

### All three services without Docker

```bash
./gradlew bootJar
docker compose up -d activemq                                  # the broker, on localhost:61616
java -jar discovery-server/build/libs/discovery-server.jar     # http://localhost:8761
java -jar trainer-workload/build/libs/trainer-workload.jar     # registers as TRAINER-WORKLOAD
./gradlew bootRun                                              # Gym CRM (local profile, needs PostgreSQL)
```

The local profiles of Gym CRM and Trainer Workload share a development-only service-token secret; on servers
both get the same `WORKLOAD_JWT_SECRET`. Both connect to the broker at `ACTIVEMQ_URL` (default
`tcp://localhost:61616`) as `ACTIVEMQ_USER` / `ACTIVEMQ_PASSWORD`; the compose file sets these for the
`activemq` container.

## Running in Docker

Each service has its own multi-stage Dockerfile (`Dockerfile`, `trainer-workload/Dockerfile`,
`discovery-server/Dockerfile`): the first stage compiles the jar with Gradle inside a JDK container, the second
contains only a JRE and the jar and runs as a non-root user. All three are built from the repository root,
because the Gradle build spans the three folders.

### Everything with Docker Compose (`compose.yml`)

```bash
docker compose up -d --build       # postgres, activemq, discovery-server, trainer-workload, gym-crm on gymcrm-net
docker compose ps                  # wait until every service shows "(healthy)"
```

- Eureka dashboard: http://localhost:8761 lists `GYM-CRM` and `TRAINER-WORKLOAD`, both `UP`.
- ActiveMQ console: http://localhost:8161/admin/queues.jsp (admin / admin) shows the queue
  `trainer-workload.updates` (messages enqueued and dequeued, consumers) and the dead letter queue.
- Containers talk to the database and to Eureka by their compose service names (`postgres`,
  `discovery-server`). Gym CRM calls the workload service by its Eureka name, `http://trainer-workload`; the
  load balancer looks up the instance's current address in Eureka. No IP address is written anywhere.
- Start-up order: the workload service and Gym CRM wait until Eureka and the broker (and Gym CRM until
  PostgreSQL) pass their health checks, not just until the containers started. Gym CRM doesn't wait for the
  workload service.
- Ports and secrets can be changed: `APP_PORT`, `WORKLOAD_PORT`, `EUREKA_PORT`, `ACTIVEMQ_CONSOLE_PORT`,
  `WORKLOAD_JWT_SECRET`, `ACTIVEMQ_USER`, `ACTIVEMQ_PASSWORD`, `ACTIVEMQ_WEB_USER`, `ACTIVEMQ_WEB_PASSWORD`
  (e.g. in a `.env` file next to `compose.yml`).
- The broker keeps its messages in the `activemq-data` volume, so queued trainings survive a restart.

Try the integration:

```bash
# register a trainee and a trainer, add a training as the trainee (see "Try it" above), then as the trainer:
curl -H "Authorization: Bearer $TRAINER_TOKEN" localhost:8080/api/trainers/Anna.Lee/workload
docker compose logs gym-crm trainer-workload | grep <X-Transaction-Id of the request>   # both services

# The workload service is down: adding trainings still works, the messages wait in the queue.
docker compose stop trainer-workload
#   add trainings (200); the console shows them as "pending" in trainer-workload.updates
#   reading a workload answers 503 (first on connect, then the circuit opens and refuses immediately)
docker compose start trainer-workload
#   it processes the waiting messages at once; it registers again within seconds, reads answer 200 again

# The broker is down: adding a training answers 503 and saves nothing; everything else keeps working.
docker compose stop activemq
docker compose start activemq    # Gym CRM reconnects with the next message, the listener within 5 s

# An invalid message (required information missing) goes to the dead letter queue with the reason.
# Send it through the broker's REST API (or the console's "Send" page):
curl -u admin:admin "http://localhost:8161/api/message/trainer-workload.updates?type=queue" \
  --data-urlencode 'body={"trainerUsername":"Anna.Lee","trainingDuration":60,"actionType":"ADD"}'
docker compose logs trainer-workload | grep "dead letter"
#   ... Invalid message: isActive must not be null, trainerFirstName must not be blank, ...
#   the console lists it in trainer-workload.updates.DLQ, with the reason in the deadLetterReason property

# Eureka restarts: both services register again by themselves (heartbeat every 5 s).
docker compose restart discovery-server
```

```bash
docker compose logs -f trainer-workload   # one service's logs
docker compose down                       # add -v to also delete the database volume
```

Each image also runs on its own:

```bash
docker run -d -p 8080:8080 gym-crm:1.0                    # standalone profile: H2, no Eureka, no workload calls
docker run -d -p 8081:8081 -e WORKLOAD_JWT_SECRET=... -e EUREKA_URL=http://<eureka-host>:8761/eureka \
  -e ACTIVEMQ_URL=tcp://<broker-host>:61616 -e ACTIVEMQ_USER=... -e ACTIVEMQ_PASSWORD=... trainer-workload:1.0
docker run -d -p 8761:8761 gymcrm-discovery-server:1.0
```

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

With plain `docker` commands (Compose does this, plus the other services, see above):

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
| 19 | Trainer's minutes per month        | `GET /api/trainers/{username}/workload`            | 200     |

Adding a training (14) reports it to the Trainer Workload service (`ADD`). A training can only be deleted by
deleting its trainee (7: the trainee's trainings are deleted with them), which reports each of the trainee's
trainings (`DELETE`). The reports are messages on an ActiveMQ queue:

- **Asynchronous.** Gym CRM answers without waiting for the workload service; if that service is down, the
  messages wait in the queue (stored on disk by the broker) and are processed when it is back.
- **Only for committed changes.** The messages are sent in a JMS transaction tied to the database transaction:
  they reach the queue only after the commit, and are dropped on a rollback. Deleting a trainee sends all of
  their trainings' `DELETE`s or none.
- **Broker down.** If the broker can't be reached, these two requests answer **503** and change nothing; all
  other requests keep working.
- **In order.** The workload service processes the queue with a single consumer, so a training's `DELETE` can't
  overtake its `ADD`.

Reading a workload (19) stays a synchronous REST call, because the caller needs the answer right away.

### Trainer Workload API (port 8081)

| Operation                                  | Method and URL                                                     | Scope            |
|--------------------------------------------|--------------------------------------------------------------------|------------------|
| A trainer's minutes per year and month     | `GET /api/trainer-workloads/{username}`                            | `workload.read`  |
| A trainer's minutes in one month           | `GET /api/trainer-workloads/{username}/years/{year}/months/{month}` | `workload.read`  |

Added and deleted trainings arrive on the queue `trainer-workload.updates` as JSON text messages, with Gym
CRM's transaction id in the `transactionId` message property:

```json
{"trainerUsername":"Anna.Lee","trainerFirstName":"Anna","trainerLastName":"Lee","isActive":true,
 "trainingDate":"2026-11-20","trainingDuration":60,"actionType":"ADD"}
```

Every field is required. A message that can't be processed goes to the dead letter queue
`trainer-workload.updates.DLQ`, with its original body and the reason in the `deadLetterReason` property:

- **Invalid** (not JSON, a field missing or invalid): moved at once, since retrying can't fix it.
  Example reason: `Invalid message: trainingDate must not be null`.
- **Processing failed**: the transaction is rolled back and ActiveMQ delivers the message again after 1 s,
  2 s, 4 s; if the 4th attempt fails too, it is moved with a reason such as `Failed 4 times: ...`.

A dead letter doesn't block the queue: the next message is processed normally.

A workload response:

```json
{"trainerUsername":"Anna.Lee","trainerFirstName":"Anna","trainerLastName":"Lee","trainerStatus":"ACTIVE",
 "years":[{"year":2026,"months":[{"month":11,"trainingSummaryDuration":105},{"month":12,"trainingSummaryDuration":30}]}]}
```

Only Gym CRM can call it: each call carries a short-lived JWT (5 minutes) signed with `WORKLOAD_JWT_SECRET`, a
secret the two services share and that is separate from the one for user tokens. The token must have issuer
`gym-crm`, audience `trainer-workload` and the scope `workload.read`. The queue is protected by the broker's
login instead. Every request gets the caller's
`X-Transaction-Id`, so one request can be followed through the logs of both services.

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
| 502    | Reading a workload: the workload service rejected the call (e.g. different `WORKLOAD_JWT_SECRET`s) |
| 503    | Adding a training or deleting a trainee: the message broker is down, nothing is saved. Reading a workload: the workload service is down, too slow, or its circuit is open |

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
- **No broker health check.** Spring's JMS health check is turned off in both services
  (`management.health.jms.enabled: false`): a broker outage only stops adding trainings and deleting trainees,
  so it shouldn't mark the whole service `DOWN` for Docker and Eureka.

## Project structure

```
io.gymcrm
├── Application                  the Spring Boot entry point
├── config                       RequiredSettingsCheck: stops the start-up if a required setting is missing
├── integration.workload         TrainerWorkloadPublisher (JMS messages, or disabled) and WorkloadMessagingConfig;
│                                TrainerWorkloadClient for reads (REST + circuit breaker, or disabled), its
│                                config, WorkloadTokenProvider (service JWT), the event and response records
├── security                     SecurityConfig (filter chain, CORS, BCrypt), JwtConfig, JwtTokenService,
│                                TokenRevocationService, LoginAttemptService, GymUserDetailsService,
│                                JwtLogoutHandler, SecurityErrorHandler
├── entities                     User, Trainee, Trainer, Training, TrainingType (JPA)
├── dto                          facade inputs and results: Credentials, IssuedToken, CreatedProfile, ...
├── exceptions                   AuthenticationException (401), NotFoundException (404),
│                                ConflictException (409), LoginBlockedException (429),
│                                BadGatewayException (502), ServiceUnavailableException (503)
├── dao                          DAOs using the EntityManager and the Criteria API
├── services                     business logic, validation, @PreAuthorize rules, @Transactional
├── facade                       GymFacade: single entry point, delegates to the services
├── health                       the custom health indicators; EurekaHealthConfig reports WARNING to Eureka as UP
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

```
trainer-workload: io.gymcrm.workload
├── messaging                    TrainerWorkloadListener (the queue's consumer), WorkloadMessageReader (JSON +
│                                validation), DeadLetterQueue, MessagingConfig (redelivery with back-off)
├── model                        TrainerWorkload (years → months → minutes), ActionType
├── repository                   InMemoryTrainerWorkloadRepository: ConcurrentHashMap, atomic per trainer
├── service                      TrainerWorkloadServiceImpl: ADD/DELETE, reading
├── security                     SecurityConfig: JWT check (issuer, audience, scopes)
└── web                          controller, records, GlobalExceptionHandler, transaction id + call logging

discovery-server: io.gymcrm.discovery.DiscoveryServerApplication (@EnableEurekaServer)
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
- **`WorkloadMessagingIntegrationTest`** runs Gym CRM against an in-memory ActiveMQ broker: the exact message
  body and its transaction id, one `DELETE` per training when a trainee is deleted, and no message until the
  database transaction commits (none at all on a rollback).
- **`BrokerUnavailableIntegrationTest`** points Gym CRM at a broker that doesn't exist: adding a training answers
  503 and saves nothing, while the other endpoints and the health check keep working.
- **`TrainerWorkloadIntegrationTest`** runs Gym CRM with the real workload client against a fake workload
  service (the JDK's HTTP server), found by name through the load balancer: reading a trainer's workload with
  the `workload.read` token and the transaction id, a timeout (503), a rejection (401 → 502, the circuit stays
  closed), the circuit opening, and the trainer's workload endpoint (who may see it, empty workloads, status).
- **`DisabledWorkloadIntegrationTest`** checks that with the integration disabled nothing is sent or called.
- **Trainer Workload**: unit tests for the model, the repository (including concurrent updates), the service,
  the controller, `WorkloadMessageReaderTest` and `TrainerWorkloadListenerTest`, and two integration tests:
  - `WorkloadApiIntegrationTest` with real tokens (missing, forged, expired, wrong issuer, audience or scope);
    a POST answers 403, since updates only arrive through the queue.
  - `WorkloadMessagingIntegrationTest` with an in-memory broker: messages processed in order, invalid or
    unreadable messages moved to the dead letter queue, a failure retried and then processed, and a message
    that keeps failing moved after 4 attempts without blocking the next one.
- **`ApiDocumentationTest`** checks that the Swagger annotations are complete and agree with the code.

## Known limitations

- The Trainer Workload service keeps its data in memory: a restart loses it, and trainings added before are
  then missing there (a later `DELETE` of one is logged as a warning and never makes a month negative).
- **A message can be processed twice.** ActiveMQ delivers at least once: if the workload service applies a
  message but crashes before acknowledging it, the message is delivered again and counted twice. A message id
  remembered by the workload service (an idempotent consumer) would prevent this.
- **A message can be lost in one narrow case.** The database commits first, then the JMS transaction; if the
  broker fails between the two, the training is saved but its message is gone. A transactional outbox (the
  message saved in the database with the training, sent by a separate job) would close this gap.
- **Reads are eventually consistent.** Right after adding a training, reading the trainer's workload can still
  show the old total until the message has been processed.
- While the broker is unreachable, adding a training holds its database connection for up to the broker's
  2 s connection timeout before answering 503.

- Failed-login counts and revoked tokens are kept in memory: they are lost on restart and not shared between
  instances. Running several instances would need a shared store such as Redis.
- Existing tokens stay valid after a password change, until they expire or the user logs out.
- Schema changes to an existing database aren't handled: `CREATE TABLE IF NOT EXISTS` skips existing tables. A
  migration tool such as Flyway would solve this.
- There is no Swagger UI yet; adding `springdoc-openapi-starter-webmvc-ui` would provide it.
