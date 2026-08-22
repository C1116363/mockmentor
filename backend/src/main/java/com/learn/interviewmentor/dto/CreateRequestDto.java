package com.learn.interviewmentor.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

/**
 * What the candidate sends in when booking.
 *
 * The student is taken from the token, which is why there is no name or email.
 */
@Schema(description = "Book a mock interview in a one-hour slot.")
public record CreateRequestDto(

        @Schema(description = "What the interview should cover",
                example = "Spring Boot backend round", maxLength = 150)
        @NotBlank(message = "Tell us what you want to be interviewed on")
        @Size(max = 150)
        String topic,

        @Schema(description = "Your experience level",
                example = "Fresher",
                allowableValues = {"Fresher", "0-1 years", "1-3 years", "3-5 years", "5+ years"})
        @NotBlank(message = "Experience level is required")
        String experienceLevel,

        @Schema(description = "Start of the one-hour slot you picked. Must be on the hour and "
                + "come from GET /api/slots.",
                example = "2026-09-20T15:00:00", type = "string", format = "date-time")
        @NotNull(message = "Pick a slot")
        @Future(message = "Pick a slot in the future")
        LocalDateTime preferredSlot,

        @Schema(description = "Anything the interviewer should know beforehand (optional)",
                example = "Final year student, weak on JPA relationships.", maxLength = 1000)
        @Size(max = 1000)
        String notes
) {
}
