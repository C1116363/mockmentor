package com.learn.interviewmentor.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Describes the API to Swagger.
 *
 * The important bit is the "bearerAuth" security scheme. It puts an
 * "Authorize" button in Swagger UI: paste a token from /api/auth/login once and
 * every protected endpoint below it is called with the Authorization header
 * already attached. Without this you would get 401 on everything and assume the
 * API was broken.
 *
 * addSecurityItem() applies the scheme to EVERY endpoint by default. The few
 * genuinely public ones opt out with @SecurityRequirements (note the plural)
 * on the method.
 */
@Configuration
public class OpenApiConfig {

    private static final String SCHEME_NAME = "bearerAuth";

    @Bean
    public OpenAPI abhiMentorOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("AbhiMentor API")
                        .version("1.0.0")
                        .description("""
                                Students and working professionals request mock interviews;
                                senior mentors pick them up and take the call.

                                ### Getting started
                                1. Call `POST /api/auth/login` with one of the demo accounts
                                   (password `password123` for all of them):
                                   - `rahul@example.com` — STUDENT
                                   - `ananya@example.com` — MENTOR
                                   - `admin@example.com` — ADMIN
                                2. Copy the `token` from the response.
                                3. Click **Authorize** at the top right and paste it in.
                                4. Every endpoint now sends `Authorization: Bearer <token>`.

                                ### Status codes you will see
                                - **401 Unauthorized** — no token, expired token, or bad signature.
                                - **403 Forbidden** — you are logged in, but your role (or ownership)
                                  does not allow that action.
                                """)
                        .contact(new Contact().name("AbhiMentor").email("support@example.com"))
                        .license(new License().name("MIT")))

                .servers(List.of(
                        new Server().url("http://localhost:8080").description("Local development")))

                .addSecurityItem(new SecurityRequirement().addList(SCHEME_NAME))

                .components(new Components().addSecuritySchemes(SCHEME_NAME,
                        new SecurityScheme()
                                .name(SCHEME_NAME)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Paste the raw token from /api/auth/login. "
                                        + "Do not type the word 'Bearer' - Swagger adds it.")));
    }
}
