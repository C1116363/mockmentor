package com.learn.interviewmentor.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Create a MENTOR account and its profile in one call.")
public record MentorSignupRequest(

        @Schema(description = "Your full name", example = "Arjun Nair", maxLength = 100)
        @NotBlank(message = "Name is required")
        @Size(max = 100)
        String fullName,

        @Schema(description = "Must not already be registered", example = "arjun@example.com")
        @NotBlank(message = "Email is required")
        @Email(message = "Must be a valid email")
        String email,

        @Schema(description = "At least 8 characters", example = "secret123", minLength = 8)
        @NotBlank(message = "Password is required")
        @Size(min = 8, message = "Password must be at least 8 characters")
        String password,

        @Schema(description = "Comma-separated areas you can interview on",
                example = "Java, Spring Boot, System Design", maxLength = 200)
        @NotBlank(message = "List at least one area of expertise")
        @Size(max = 200)
        String expertise,

        @Schema(description = "Mentors need at least 3 years", example = "8", minimum = "3")
        @Min(value = 3, message = "Mentors need at least 3 years of experience")
        int yearsOfExperience,

        @Schema(description = "Where you work now (optional)", example = "Flipkart", maxLength = 100)
        @Size(max = 100)
        String currentCompany,

        @Schema(description = "A short intro shown to students (optional)",
                example = "Backend engineer. Happy to go deep on JPA and API design.", maxLength = 500)
        @Size(max = 500)
        String bio
) {
}
