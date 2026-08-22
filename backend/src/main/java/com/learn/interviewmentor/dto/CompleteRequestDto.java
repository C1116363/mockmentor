package com.learn.interviewmentor.dto;

import com.learn.interviewmentor.model.Recommendation;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * The mentor's scorecard, filled in when they close an interview.
 *
 * The summary and the overall rating are required - a scorecard with neither
 * tells the candidate nothing. Everything else is optional, because not every
 * round covers every dimension.
 */
@Schema(description = "Scorecard for a completed interview.")
public record CompleteRequestDto(

        @Schema(description = "Overall summary the candidate reads first",
                example = "Strong on annotations and the request lifecycle. Struggled once we "
                        + "got into transaction boundaries.", maxLength = 2000)
        @NotBlank(message = "Write a short summary - it's the main thing they'll read")
        @Size(max = 2000)
        String feedback,

        @Schema(description = "What went well", example = "Explained their reasoning out loud "
                + "without being prompted. Clean, readable code.", maxLength = 2000)
        @Size(max = 2000)
        String strengths,

        @Schema(description = "What to work on before the real thing",
                example = "Revise fetch types and the N+1 problem. Practise talking through "
                        + "trade-offs rather than jumping to an answer.", maxLength = 2000)
        @Size(max = 2000)
        String improvements,

        @Schema(description = "Overall score out of 5", example = "4", minimum = "1", maximum = "5")
        @NotNull(message = "Give an overall rating")
        @Min(value = 1, message = "Rating is out of 5")
        @Max(value = 5, message = "Rating is out of 5")
        Integer overallRating,

        @Schema(description = "Technical knowledge, out of 5", example = "4")
        @Min(value = 1, message = "Rating is out of 5")
        @Max(value = 5, message = "Rating is out of 5")
        Integer technicalRating,

        @Schema(description = "Communication, out of 5", example = "5")
        @Min(value = 1, message = "Rating is out of 5")
        @Max(value = 5, message = "Rating is out of 5")
        Integer communicationRating,

        @Schema(description = "Problem solving, out of 5", example = "3")
        @Min(value = 1, message = "Rating is out of 5")
        @Max(value = 5, message = "Rating is out of 5")
        Integer problemSolvingRating,

        @Schema(description = "Overall verdict", example = "ALMOST_READY",
                allowableValues = {"READY", "ALMOST_READY", "NEEDS_WORK"})
        @NotNull(message = "Pick a verdict")
        Recommendation recommendation
) {
}
