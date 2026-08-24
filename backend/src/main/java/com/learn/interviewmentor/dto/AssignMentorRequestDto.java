package com.learn.interviewmentor.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

/** An admin attaching a specific mentor to a student's request. */
@Schema(description = "Assign a verified mentor to a pending interview request.")
public record AssignMentorRequestDto(

        @Schema(description = "User id of the mentor to attach. Must be a verified (APPROVED) mentor.",
                example = "2")
        @NotNull(message = "Pick a mentor")
        Long mentorId,

        @Schema(description = "When the interview will happen. Leave this out and the student's "
                + "own requested slot is used.",
                example = "2026-09-20T15:00:00", type = "string", format = "date-time")
        LocalDateTime scheduledAt,

        @Schema(description = "Optional. **Leave this blank and a meeting room is created "
                + "automatically** when the mentor is assigned. Only set it if you want to use "
                + "a link you made yourself.",
                example = "https://meet.google.com/abc-defg-hij")
        String meetingLink
) {
}
