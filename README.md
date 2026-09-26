# Gym CRM — Spring Core

## Tech stack

- Java 17
- Spring Framework (`spring-context`), configured with annotations
- Jackson (with `jackson-datatype-jsr310` for `LocalDate` / `Duration`) for reading seed data files
- Lombok
- SLF4J + Logback for logging
- JUnit 5 + Mockito for tests
- Gradle

## Running

Run `io.gymcrm.Application` from the IDE. To run the tests:

```bash
./gradlew test
```

`Application` starts the Spring context, prints how much seed data from file was loaded, shows
creating a trainee, a trainer and a training, including a situation when two people have the same username,
for example a second `John Smith`.

## Project structure

```
io.gymcrm
├── Application                  entry point / demo
├── config
│   ├── AppConfig
│   ├── StorageConfig            one Map bean per entity type
│   └── StorageNames             bean names of the storage maps
├── entities                     User (abstract), Trainee, Trainer, Training, TrainingType
├── storage
│   └── StorageInitializer       BeanPostProcessor that fills the storage from files
├── dao                          TraineeDao, TrainerDao, TrainingDao and implementations
├── services                     TraineeService, TrainerService, TrainingService and their implementations
├── facade
│   └── GymFacade                single entry point to all operations
└── util
    ├── UsernameGenerator
    └── PasswordGenerator
```

## Operations

| Service           | Create | Update | Delete | Select |
|-------------------|:------:|:------:|:------:|:------:|
| `TraineeService`  |   ✔    |   ✔    |   ✔    |   ✔    |
| `TrainerService`  |   ✔    |   ✔    |        |   ✔    |
| `TrainingService` |   ✔    |        |        |   ✔    |

Select supports lookup by id, by username (trainees and trainers), and listing all records.
All operations are exposed through `GymFacade`.

**Configuration.** The application context is built from annotations: `AppConfig` scans the
`io.gymcrm` package and loads `application.properties`.

**Initial data.** `StorageInitializer` implements `BeanPostProcessor`. After each storage bean is
created, it reads the matching JSON file and fills the map. The file paths come from
`application.properties`:

```properties
storage.trainee.file=classpath:data/trainees.json
storage.trainer.file=classpath:data/trainers.json
storage.training.file=classpath:data/trainings.json
```

**Injection.**
- Storage maps are injected into DAOs, and DAOs into services, through `@Autowired` setters.
- Services are injected into `GymFacade` through its constructor.
- All other injections are setter-based.

**Username and password.**
- The username is `firstName.lastName`, e.g. `John.Smith`.
- If that username is already taken by any trainee **or** trainer, a serial number is added:
  `John.Smith1`, `John.Smith2`, and so on. The generator uses the lowest free number, so it never
  produces a duplicate even after users are deleted.
- The password is a random 10-character alphanumeric string generated with `SecureRandom`.

## Tests

The test suite (138 tests) covers:

- DAOs: create, update, delete, lookups, and that `findAll` returns an unmodifiable snapshot
- `UsernameGenerator`: free names, clashes with trainees and trainers, serial numbers, gaps, whitespace
- `PasswordGenerator`: length, allowed characters, uniqueness
- Services: credential generation, validation, credentials preserved on update, not-found cases,
  training rules (existing trainee/trainer, matching specialization, positive duration)
- `GymFacade`: every method delegates to the right service
- `StorageInitializer`: loading each file, ignoring other beans, missing and malformed files
- Entities: id-based equality, password hidden in `toString`, protected specialization set
- Integration test: starts the full Spring context against test seed data in
  `src/test/resources/test-data` and runs end-to-end scenarios