package services.implementations;

import io.gymcrm.dao.TraineeDao;
import io.gymcrm.dao.TrainerDao;
import io.gymcrm.dto.CreatedProfile;
import io.gymcrm.dto.IssuedToken;
import io.gymcrm.dto.TraineeRegistration;
import io.gymcrm.dto.TraineeUpdate;
import io.gymcrm.entities.Trainee;
import io.gymcrm.entities.Trainer;
import io.gymcrm.exceptions.ConflictException;
import io.gymcrm.exceptions.NotFoundException;
import io.gymcrm.security.JwtTokenService;
import io.gymcrm.security.Role;
import io.gymcrm.services.implementations.TraineeServiceImpl;
import io.gymcrm.util.PasswordGenerator;
import io.gymcrm.util.UsernameGenerator;
import io.gymcrm.metrics.GymMetrics;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import support.TestData;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static support.TestData.FITNESS;
import static support.TestData.YOGA;
import static support.TestData.trainee;
import static support.TestData.trainer;

@ExtendWith(MockitoExtension.class)
class TraineeServiceImplTest {


    @Mock
    private TraineeDao traineeDao;

    @Mock
    private TrainerDao trainerDao;

    @Mock
    private UsernameGenerator usernameGenerator;

    @Mock
    private PasswordGenerator passwordGenerator;

    @Mock
    private JwtTokenService tokenService;

    private TraineeServiceImpl service;

    private final SimpleMeterRegistry registry = new SimpleMeterRegistry();

    @BeforeEach
    void setUp() {
        service = new TraineeServiceImpl();
        service.setMetrics(new GymMetrics(registry));
        service.setPasswordEncoder(TestData.ENCODER);
        service.setTraineeDao(traineeDao);
        service.setTrainerDao(trainerDao);
        service.setUsernameGenerator(usernameGenerator);
        service.setPasswordGenerator(passwordGenerator);
        service.setTokenService(tokenService);
    }

    private Trainee givenJohn() {
        Trainee john = trainee("John.Smith", "secret");
        when(traineeDao.findByUsername("John.Smith")).thenReturn(Optional.of(john));
        return john;
    }

    @Nested
    class Create {

        @Test
        void generatesCredentialsAndActivates() {
            when(usernameGenerator.generate("John", "Smith")).thenReturn("John.Smith");
            when(passwordGenerator.generate()).thenReturn("aaaaaaaaaa");
            IssuedToken token = new IssuedToken("jwt", 3600);
            when(tokenService.issue("John.Smith", Role.TRAINEE)).thenReturn(token);
            when(traineeDao.save(any())).thenAnswer(inv -> inv.getArgument(0));

            CreatedProfile<Trainee> result = service.create(
                    new TraineeRegistration(" John ", "Smith", LocalDate.of(1995, 4, 12), "Tashkent"));
            Trainee created = result.profile();

            assertEquals("John", created.getUser().getFirstName());
            assertEquals("John.Smith", created.getUser().getUsername());
            assertEquals("aaaaaaaaaa", result.password(), "the generated password is returned once");
            assertSame(token, result.token(), "a token for immediate use");
            assertNotEquals("aaaaaaaaaa", created.getUser().getPassword(), "only a hash is stored");
            assertTrue(TestData.ENCODER.matches("aaaaaaaaaa", created.getUser().getPassword()));
            assertTrue(created.getUser().isActive());
            assertEquals(LocalDate.of(1995, 4, 12), created.getDateOfBirth());
            assertEquals("Tashkent", created.getAddress());
            assertEquals(1, registry.get(GymMetrics.REGISTRATIONS).tag("role", "trainee").counter().count());
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = "   ")
        void rejectsMissingFirstName(String firstName) {
            assertThrows(IllegalArgumentException.class,
                    () -> service.create(new TraineeRegistration(firstName, "Smith", null, null)));
            verifyNoInteractions(traineeDao, usernameGenerator, passwordGenerator);
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = "   ")
        void rejectsMissingLastName(String lastName) {
            assertThrows(IllegalArgumentException.class,
                    () -> service.create(new TraineeRegistration("John", lastName, null, null)));
            verifyNoInteractions(traineeDao, usernameGenerator, passwordGenerator);
        }

        @Test
        void rejectsNullRegistration() {
            assertThrows(NullPointerException.class, () -> service.create(null));
            verifyNoInteractions(traineeDao);
            assertEquals(0, registry.get(GymMetrics.REGISTRATIONS).tag("role", "trainee").counter().count());
        }
    }

    @Nested
    class GetByUsername {

        @Test
        void anyAuthenticatedUserCanReadAProfile() {
            Trainee john = givenJohn();
            assertSame(john, service.getByUsername("John.Smith"));
        }

        @Test
        void unknownTrainee() {
            when(traineeDao.findByUsername("Nobody")).thenReturn(Optional.empty());

            assertThrows(NotFoundException.class, () -> service.getByUsername("Nobody"));
        }
    }

    @Nested
    class Update {

        @Test
        void changesFieldsButKeepsUsernameAndPassword() {
            Trainee john = givenJohn();

            service.update("John.Smith",
                    new TraineeUpdate("Johnny", "Smithson", LocalDate.of(2000, 1, 1), "Bukhara", false));

            assertEquals("Johnny", john.getUser().getFirstName());
            assertEquals("Smithson", john.getUser().getLastName());
            assertEquals(LocalDate.of(2000, 1, 1), john.getDateOfBirth());
            assertEquals("Bukhara", john.getAddress());
            assertFalse(john.getUser().isActive());
            assertEquals("John.Smith", john.getUser().getUsername());
            assertEquals("secret", john.getUser().getPassword());
        }

        @Test
        void optionalFieldsCanBeCleared() {
            Trainee john = givenJohn();
            john.setAddress("Tashkent");

            service.update("John.Smith", new TraineeUpdate("John", "Smith", null, null, true));

            assertNull(john.getAddress());
        }

        @Test
        void rejectsMissingRequiredFields() {
            assertThrows(IllegalArgumentException.class,
                    () -> service.update("John.Smith", new TraineeUpdate("John", null, null, null, true)));
            assertThrows(IllegalArgumentException.class,
                    () -> service.update("John.Smith", new TraineeUpdate(" ", "Smith", null, null, true)));
            assertThrows(IllegalArgumentException.class,
                    () -> service.update("John.Smith", new TraineeUpdate("John", "Smith", null, null, null)));
            assertThrows(NullPointerException.class, () -> service.update("John.Smith", null));
            verifyNoInteractions(traineeDao);
        }

        @Test
        void traineeDeletedAfterAuthentication() {
            when(traineeDao.findByUsername("John.Smith")).thenReturn(Optional.empty());

            assertThrows(NotFoundException.class, () -> service.update("John.Smith",
                    new TraineeUpdate("John", "Smith", null, null, true)));
        }
    }

    @Nested
    class SetActive {

        @Test
        void changesTheFlag() {
            Trainee john = givenJohn();

            service.setActive("John.Smith", false);
            assertFalse(john.getUser().isActive());

            service.setActive("John.Smith", true);
            assertTrue(john.getUser().isActive());
        }

        @Test
        void sameStateIsAConflict() {
            Trainee john = givenJohn();

            ConflictException e = assertThrows(ConflictException.class,
                    () -> service.setActive("John.Smith", true));

            assertEquals("Trainee John.Smith is already active", e.getMessage());
            assertTrue(john.getUser().isActive());
        }
    }

    @Nested
    class Delete {

        @Test
        void deletesOwnProfile() {
            Trainee john = givenJohn();

            service.delete("John.Smith");

            verify(traineeDao).delete(john);
        }

        @Test
        void traineeDeletedAfterAuthentication() {
            when(traineeDao.findByUsername("John.Smith")).thenReturn(Optional.empty());

            assertThrows(NotFoundException.class, () -> service.delete("John.Smith"));
            verify(traineeDao, never()).delete(any());
        }
    }

    @Nested
    class UpdateTrainers {

        @Test
        void replacesTheList() {
            Trainee john = givenJohn();
            john.getTrainers().add(trainer("Old.Trainer", "pw", FITNESS));
            Trainer anna = trainer("Anna.Lee", "pw", YOGA);
            when(trainerDao.findByUsernames(Set.of("Anna.Lee"))).thenReturn(List.of(anna));

            List<Trainer> result = service.updateTrainers("John.Smith", List.of("Anna.Lee", "Anna.Lee"));

            assertEquals(List.of(anna), result);
            assertEquals(Set.of(anna), john.getTrainers());
        }

        @Test
        void emptyListRemovesAllTrainers() {
            Trainee john = givenJohn();
            john.getTrainers().add(trainer("Old.Trainer", "pw", FITNESS));
            when(trainerDao.findByUsernames(Set.of())).thenReturn(List.of());

            assertTrue(service.updateTrainers("John.Smith", List.of()).isEmpty());
            assertTrue(john.getTrainers().isEmpty());
        }

        @Test
        void unknownTrainerLeavesTheListUnchanged() {
            Trainee john = givenJohn();
            Trainer old = trainer("Old.Trainer", "pw", FITNESS);
            john.getTrainers().add(old);
            when(trainerDao.findByUsernames(anyCollection())).thenReturn(List.of(trainer("Anna.Lee", "pw", YOGA)));

            NotFoundException e = assertThrows(NotFoundException.class,
                    () -> service.updateTrainers("John.Smith", List.of("Anna.Lee", "Nobody")));

            assertEquals("Trainers not found: [Nobody]", e.getMessage());
            assertEquals(Set.of(old), john.getTrainers());
        }

        @Test
        void rejectsNullList() {
            assertThrows(NullPointerException.class, () -> service.updateTrainers("John.Smith", null));
            verifyNoInteractions(traineeDao, trainerDao);
        }
    }
}
