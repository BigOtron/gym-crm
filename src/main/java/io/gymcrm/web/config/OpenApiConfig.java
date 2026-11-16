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
                        Authentication: HTTP Basic (`Authorization: Basic base64(username:password)`) \
                        on every endpoint except the two registrations and the password change."""),
        security = @SecurityRequirement(name = OpenApiConfig.BASIC_AUTH))
@SecurityScheme(name = OpenApiConfig.BASIC_AUTH, type = SecuritySchemeType.HTTP, scheme = "basic")
public final class OpenApiConfig {

    public static final String BASIC_AUTH = "basicAuth";

    private OpenApiConfig() {
    }
}
