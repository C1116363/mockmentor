package com.learn.interviewmentor.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Creating an account - identical for candidates and mentors.
 *
 * Mentors used to send their expertise and experience here. Those moved to
 * PUT /api/mentor/profile when verification was introduced, so signup is the
 * same three fields for everyone. Keeping one record means the two endpoints
 * cannot drift apart again.
 */
@Schema(description = "Create an account. Same fields whether you are signing up "
        + "as a candidate or as a mentor.")
public record SignupRequestDto(

        @Schema(description = "Your full name", example = "Priya Menon", maxLength = 100)
        @NotBlank(message = "Name is required")
        @Size(max = 100)
        String fullName,

        @Schema(description = "Must not already be registered", example = "priya@example.com")
        @NotBlank(message = "Email is required")
        @Email(message = "Must be a valid email")
        String email,

        @Schema(description = "At least 8 characters. Stored as a BCrypt hash, never in plain text.",
                example = "secret123", minLength = 8)
        @NotBlank(message = "Password is required")
        @Size(min = 8, message = "Password must be at least 8 characters")
        String password
) {
}
