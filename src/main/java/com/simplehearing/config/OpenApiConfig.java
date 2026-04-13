package com.simplehearing.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.tags.Tag;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    private static final String BEARER_SCHEME = "bearerAuth";

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Simple Hearing & Speech Care API")
                        .description("""
                                Multi-clinic SaaS backend for speech and hearing therapy practice management.

                                **Roles:**
                                - `ADMIN` — clinic owner / doctor: full access within their clinic
                                - `THERAPIST` — manages assigned patients, sessions, homework and reports
                                - `PATIENT` / `PARENT` — read-only access to their own progress data

                                **Authentication:** All endpoints (except `/api/v1/auth/login` and `/api/v1/auth/refresh`) \
                                require a Bearer JWT in the `Authorization` header.
                                """)
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Simple Hearing & Speech Care")
                                .email("admin@simplehearing.in")))
                .servers(List.of(
                        new Server().url("http://localhost:8080").description("Local development"),
                        new Server().url("https://api.simplehearing.in").description("Production")
                ))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_SCHEME))
                .components(new Components()
                        .addSecuritySchemes(BEARER_SCHEME, new SecurityScheme()
                                .name(BEARER_SCHEME)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Paste your access token here (without the 'Bearer ' prefix).")))
                .tags(List.of(
                        new Tag().name("Auth").description("Login, logout, token refresh and current-user lookup"),
                        new Tag().name("Clinics").description("Clinic registration and profile management"),
                        new Tag().name("Users").description("Onboarding and management of therapists, patients and parents"),
                        new Tag().name("Plans").description("Therapy plans, goals and milestones"),
                        new Tag().name("Appointments").description("Session scheduling and therapist availability"),
                        new Tag().name("Homework").description("Homework assignments, submissions and therapist feedback"),
                        new Tag().name("Reports").description("Periodic progress reports"),
                        new Tag().name("Media").description("File and media attachment upload / download"),
                        new Tag().name("Notifications").description("In-app notification inbox"),
                        new Tag().name("Audit").description("Audit log (admin only)")
                ));
    }
}
