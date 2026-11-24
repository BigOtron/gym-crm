package io.gymcrm.workload.web.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;

@OpenAPIDefinition(
        info = @Info(
                title = "Trainer Workload API",
                version = "1.0",
                description = """
                        Trainers' training minutes per year and month. Gym CRM keeps them up to date by \
                        sending added and deleted trainings to the ActiveMQ queue `trainer-workload.updates`.
                        Every endpoint needs a service token from Gym CRM: `Authorization: Bearer <token>` \
                        with audience `trainer-workload` and the scope `workload.read`."""),
        security = @SecurityRequirement(name = OpenApiConfig.BEARER_TOKEN))
@SecurityScheme(name = OpenApiConfig.BEARER_TOKEN, type = SecuritySchemeType.HTTP, scheme = "bearer", bearerFormat = "JWT")
public final class OpenApiConfig {

    public static final String BEARER_TOKEN = "bearerToken";

    private OpenApiConfig() {
    }
}
