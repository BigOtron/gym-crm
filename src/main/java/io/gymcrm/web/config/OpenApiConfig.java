package io.gymcrm.web.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;

@OpenAPIDefinition(
        info = @Info(
                title = "Gym CRM API",
                version = "1.0",
                description = """
                        Trainee and trainer profiles, their trainings and training types.
                        Authentication: log in with POST /api/auth/login (or register) to get a JWT, and send it \
                        as `Authorization: Bearer <token>` on every endpoint except the registrations and the \
                        login. POST /api/auth/logout revokes the token."""),
        security = @SecurityRequirement(name = OpenApiConfig.BEARER_TOKEN))
@SecurityScheme(name = OpenApiConfig.BEARER_TOKEN, type = SecuritySchemeType.HTTP, scheme = "bearer", bearerFormat = "JWT")
public final class OpenApiConfig {

    public static final String BEARER_TOKEN = "bearerToken";

    private OpenApiConfig() {
    }
}
