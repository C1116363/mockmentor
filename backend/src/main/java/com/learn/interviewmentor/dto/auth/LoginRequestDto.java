package com.learn.interviewmentor.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Credentials for logging in. Works for all three roles.")
public record LoginRequestDto(

        @Schema(description = "Your account email", example = "rahul@example.com")
        @NotBlank(message = "Email is required")
        @Email(message = "Must be a valid email")
        String email,

        @Schema(description = "Your password. All demo accounts use password123.",
                example = "password123")
        @NotBlank(message = "Password is required")
        String password
) {
}
