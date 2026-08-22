package com.learn.interviewmentor.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

/** An admin attaching a specific mentor to a student's request. */
@Schema(description = "Assign a verified mentor to a pending interview request.")
public record AssignMentorRequest(

        @Schema(description = "User id of the mentor to attach. Must be a verified (APPROVED) mentor.",
                example = "2")
        @NotNull(message = "Pick a mentor")
        Long mentorId,

        @Schema(description = "When the interview will happen. Leave this out and the student's "
                + "own requested slot is used.",
                example = "2026-09-20T15:00:00", type = "string", format = "date-time")
        LocalDateTime scheduledAt,

        @Schema(description = "Zoom / Meet / Teams link the student will join",
                example = "https://meet.google.com/abc-defg-hij")
        @NotBlank(message = "Share a meeting link so the student can join")
        String meetingLink
) {
}
