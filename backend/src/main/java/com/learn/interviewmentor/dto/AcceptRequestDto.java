package com.learn.interviewmentor.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

/**
 * What a mentor sends when they accept a request.
 *
 * mentorId used to be here too. It is gone for the same reason as the student's
 * name: the mentor is whoever is holding the token.
 */
@Schema(description = "Accept a pending request. The mentor is taken from your token, "
        + "which is why there is no mentorId field.")
public record AcceptRequestDto(

        @Schema(description = "When the interview will happen. Must be in the future. "
                + "Format yyyy-MM-dd'T'HH:mm:ss.",
                example = "2026-09-15T15:30:00", type = "string", format = "date-time")
        @NotNull(message = "Scheduled date and time is required")
        @Future(message = "Schedule the interview in the future")
        LocalDateTime scheduledAt,

        @Schema(description = "Zoom / Meet / Teams link the student will join",
                example = "https://meet.google.com/abc-defg-hij")
        @NotBlank(message = "Share a meeting link (Zoom / Meet / Teams)")
        String meetingLink
) {
}
