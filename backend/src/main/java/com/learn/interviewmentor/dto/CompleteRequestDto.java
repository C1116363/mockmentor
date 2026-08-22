package com.learn.interviewmentor.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Mentor closes the loop after the interview happened. */
@Schema(description = "Close a scheduled interview. Feedback is mandatory - it is the "
        + "whole point of the exercise for the student.")
public record CompleteRequestDto(

        @Schema(description = "What the student did well and what to work on",
                example = "Strong on annotations. Revise fetch types and the N+1 problem.",
                maxLength = 2000)
        @NotBlank(message = "Feedback is required to close an interview")
        @Size(max = 2000)
        String feedback
) {
}
