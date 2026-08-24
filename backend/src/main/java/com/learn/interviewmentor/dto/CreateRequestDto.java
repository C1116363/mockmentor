package com.learn.interviewmentor.dto;

import com.learn.interviewmentor.model.SessionType;
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
@Schema(description = "Book a one-hour slot with an expert - a mock interview, or a mentoring "
        + "discussion.")
public record CreateRequestDto(

        /*
         * Optional, and absent means MOCK_INTERVIEW. That default is what keeps
         * this a backwards-compatible addition: every client written before
         * mentoring existed sends no sessionType and keeps booking interviews,
         * exactly as it did.
         */
        @Schema(description = "MOCK_INTERVIEW for a real interview ending in a scorecard, or "
                + "MENTORING for a discussion - career advice, a code review, working through a "
                + "design. Defaults to MOCK_INTERVIEW when omitted.",
                example = "MENTORING",
                allowableValues = {"MOCK_INTERVIEW", "MENTORING"})
        SessionType sessionType,

        @Schema(description = "What the session should cover",
                example = "Spring Boot backend round", maxLength = 150)
        @NotBlank(message = "Tell us what you'd like to cover")
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

        @Schema(description = "Anything the expert should know beforehand (optional)",
                example = "Final year student, weak on JPA relationships.", maxLength = 1000)
        @Size(max = 1000)
        String notes
) {
    /** Absent means a mock interview - see the field comment. */
    public SessionType sessionTypeOrDefault() {
        return sessionType == null ? SessionType.MOCK_INTERVIEW : sessionType;
    }
}
