package com.learn.interviewmentor.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import com.learn.interviewmentor.vo.auth.UserVo;
import com.learn.interviewmentor.model.InterviewRequest;
import com.learn.interviewmentor.model.Recommendation;
import com.learn.interviewmentor.model.RequestStatus;
import com.learn.interviewmentor.model.SessionType;

import java.time.LocalDateTime;

/** What we send back to the browser for a booking. */
@Schema(description = "A booked session - mock interview or mentoring - in whatever state it is "
        + "currently in.")
public record InterviewRequestVo(
        Long id,
        UserVo student,

        @Schema(description = "MOCK_INTERVIEW or MENTORING", example = "MENTORING")
        SessionType sessionType,

        /*
         * Sent alongside the enum so every screen prints the same words. The
         * frontend could map the constant itself, but then the label lives in
         * two places and the day a third type is added one of them is wrong.
         */
        @Schema(description = "Ready to print: \"Mock interview\" or \"Mentoring session\"",
                example = "Mentoring session")
        String sessionLabel,

        @Schema(description = "Whether this kind of session ends in ratings and a verdict. The "
                + "scorecard fields below are always null when this is false.",
                example = "false")
        boolean scored,

        String topic,
        String experienceLevel,
        @Schema(description = "Start of the booked one-hour slot", example = "2026-09-20T15:00:00")
        LocalDateTime preferredSlot,

        @Schema(description = "End of that slot", example = "2026-09-20T16:00:00")
        LocalDateTime preferredSlotEnd,
        String notes,
        @Schema(description = "PENDING -> SCHEDULED -> COMPLETED, or CANCELLED", example = "PENDING")
        RequestStatus status,
        @Schema(description = "null until a mentor accepts it")
        MentorVo mentor,
        LocalDateTime scheduledAt,
        @Schema(description = "Set when a mentor accepts", example = "https://meet.google.com/abc-defg-hij")
        String meetingLink,
        @Schema(description = "Written by the mentor once COMPLETED")
        String feedback,

        @Schema(description = "What the candidate did well")
        String strengths,

        @Schema(description = "What to work on next")
        String improvements,

        @Schema(description = "Overall score out of 5", example = "4")
        Integer overallRating,

        @Schema(description = "Technical knowledge out of 5", example = "4")
        Integer technicalRating,

        @Schema(description = "Communication out of 5", example = "5")
        Integer communicationRating,

        @Schema(description = "Problem solving out of 5", example = "3")
        Integer problemSolvingRating,

        @Schema(description = "READY, ALMOST_READY or NEEDS_WORK", example = "ALMOST_READY")
        Recommendation recommendation,
        LocalDateTime createdAt
) {
    public static InterviewRequestVo from(InterviewRequest request) {
        return new InterviewRequestVo(
                request.getId(),
                UserVo.from(request.getStudent()),
                request.getSessionType(),
                request.getSessionType().getLabel(),
                request.getSessionType().isScored(),
                request.getTopic(),
                request.getExperienceLevel(),
                request.getPreferredSlot(),
                request.getPreferredSlotEnd(),
                request.getNotes(),
                request.getStatus(),
                request.getMentor() == null ? null : MentorVo.fromUserOnly(request.getMentor()),
                request.getScheduledAt(),
                request.getMeetingLink(),
                request.getFeedback(),
                request.getStrengths(),
                request.getImprovements(),
                request.getOverallRating(),
                request.getTechnicalRating(),
                request.getCommunicationRating(),
                request.getProblemSolvingRating(),
                request.getRecommendation(),
                request.getCreatedAt()
        );
    }
}
