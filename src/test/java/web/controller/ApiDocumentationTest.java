package web.controller;

import io.gymcrm.web.auth.BasicAuth;
import io.gymcrm.web.controller.AuthController;
import io.gymcrm.web.controller.TraineeController;
import io.gymcrm.web.controller.TrainerController;
import io.gymcrm.web.controller.TrainingController;
import io.gymcrm.web.controller.TrainingTypeController;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ApiDocumentationTest {

    private static final List<Class<?>> CONTROLLERS = List.of(AuthController.class, TraineeController.class,
            TrainerController.class, TrainingController.class, TrainingTypeController.class);

    private static List<Method> endpoints() {
        return CONTROLLERS.stream()
                .flatMap(c -> Arrays.stream(c.getDeclaredMethods()))
                .filter(m -> AnnotatedElementUtils.hasAnnotation(m, RequestMapping.class))
                .toList();
    }

    private static Set<String> documentedCodes(Method method) {
        return Arrays.stream(method.getAnnotationsByType(ApiResponse.class))
                .map(ApiResponse::responseCode)
                .collect(Collectors.toSet());
    }

    private static boolean hasParameter(Method method, Class<? extends java.lang.annotation.Annotation> type) {
        return Arrays.stream(method.getParameters()).anyMatch(p -> p.isAnnotationPresent(type));
    }

    @Test
    void allSeventeenEndpointsAreFound() {
        assertEquals(17, endpoints().size());
    }

    @Test
    void everyControllerHasATag() {
        CONTROLLERS.forEach(c -> assertNotNull(c.getAnnotation(Tag.class), c.getSimpleName()));
    }

    @TestFactory
    Stream<DynamicTest> everyEndpointIsDocumented() {
        return endpoints().stream().map(method -> DynamicTest.dynamicTest(
                method.getDeclaringClass().getSimpleName() + "." + method.getName(), () -> check(method)));
    }

    private static void check(Method method) {
        Operation operation = method.getAnnotation(Operation.class);
        assertNotNull(operation, "@Operation missing");
        assertFalse(operation.summary().isBlank(), "summary missing");

        Set<String> codes = documentedCodes(method);
        Arrays.stream(method.getAnnotationsByType(ApiResponse.class))
                .forEach(r -> assertFalse(r.description().isBlank(), "description missing for " + r.responseCode()));

        // The documented success code is the one the method really returns.
        ResponseStatus status = method.getAnnotation(ResponseStatus.class);
        String success = status != null ? String.valueOf(status.value().value()) : String.valueOf(HttpStatus.OK.value());
        assertTrue(codes.contains(success), "success code " + success + " not documented: " + codes);

        // Authenticated endpoints document 401 and hide the credentials parameter;
        // public ones say so with an empty @SecurityRequirements.
        boolean authenticated = hasParameter(method, BasicAuth.class);
        if (authenticated) {
            assertTrue(codes.contains("401"), "401 not documented");
            Arrays.stream(method.getParameters()).filter(p -> p.isAnnotationPresent(BasicAuth.class))
                    .forEach(p -> assertTrue(p.getAnnotation(Parameter.class) != null
                            && p.getAnnotation(Parameter.class).hidden(), "credentials parameter must be hidden"));
        }
        SecurityRequirements publicMarker = method.getAnnotation(SecurityRequirements.class);
        boolean markedPublic = publicMarker != null && publicMarker.value().length == 0;
        boolean usesOldPassword = method.getName().equals("changePassword");
        assertEquals(!authenticated, markedPublic, "public endpoints, and only those, need an empty @SecurityRequirements");
        if (!authenticated && !usesOldPassword) {
            assertFalse(codes.contains("401"), "a public endpoint can't return 401");
        }

        // Validated input can fail with 400; every path variable is described.
        if (hasParameter(method, Valid.class)) {
            assertTrue(codes.contains("400"), "400 not documented");
        }
        Arrays.stream(method.getParameters()).filter(p -> p.isAnnotationPresent(PathVariable.class))
                .forEach(p -> assertTrue(p.getAnnotation(Parameter.class) != null
                        && !p.getAnnotation(Parameter.class).description().isBlank(), "path variable not described"));
    }
}
