package com.learn.interviewmentor.dto;

import com.learn.interviewmentor.model.Recommendation;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * What the mentor writes when they close a session.
 *
 * The summary is always required - a completed session with nothing written
 * tells the student nothing.
 *
 * <b>The overall rating and the verdict are required for a mock interview and
 * meaningless for a mentoring session</b>, so they carry no @NotNull here.
 * Bean validation never sees the booking, so it cannot express "required, but
 * only for one session type" - {@code InterviewRequestService.complete} enforces
 * it instead, reading {@code SessionType.isScored()}. Dropping the annotation
 * without that check would have quietly made scorecards optional for interviews
 * too.
 */
@Schema(description = "The mentor's write-up. Ratings and a verdict are required for a "
        + "MOCK_INTERVIEW and ignored for a MENTORING session.")
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

        @Schema(description = "Overall score out of 5. Required for a MOCK_INTERVIEW, ignored "
                + "for a MENTORING session.",
                example = "4", minimum = "1", maximum = "5")
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

        @Schema(description = "Overall verdict. Required for a MOCK_INTERVIEW, ignored for a "
                + "MENTORING session.",
                example = "ALMOST_READY",
                allowableValues = {"READY", "ALMOST_READY", "NEEDS_WORK"})
        Recommendation recommendation
) {
}
