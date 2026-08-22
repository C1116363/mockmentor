package com.learn.interviewmentor.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * What the student sends in when raising a request.
 *
 * The name and email that used to be here are gone: the server takes those from
 * the logged-in account.
 */
@Schema(description = "Raise a mock-interview request. The student is taken from your token, "
        + "which is why there is no name or email field.")
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

        @Schema(description = "Cannot be in the past. Format yyyy-MM-dd.",
                example = "2026-09-15", type = "string", format = "date")
        @NotNull(message = "Preferred date is required")
        @FutureOrPresent(message = "Preferred date cannot be in the past")
        LocalDate preferredDate,

        @Schema(description = "Anything the mentor should know beforehand (optional)",
                example = "Final year student, weak on JPA relationships.", maxLength = 1000)
        @Size(max = 1000)
        String notes
) {
}
