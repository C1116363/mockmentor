package com.learn.interviewmentor.vo.plan;

import com.learn.interviewmentor.model.EnrollmentStatus;
import com.learn.interviewmentor.model.PlanEnrollment;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** A student's purchase of a plan - their view, and the admin's review queue. */
@Schema(description = "One student buying one plan, through the same manual UPI flow as interviews.")
public record PlanEnrollmentVo(

        Long id,

        Long planId,

        @Schema(example = "Placement Guide")
        String planName,

        @Schema(description = "What this student was charged. Frozen at purchase time, so a later "
                + "price change never rewrites it.", example = "2999.00")
        BigDecimal pricePaid,

        @Schema(description = "AWAITING_PAYMENT, SUBMITTED, ACTIVE, REJECTED, CANCELLED or EXPIRED")
        EnrollmentStatus status,

        @Schema(description = "ACTIVE and still inside its window")
        boolean currentlyActive,

        String upiReference,

        @Schema(description = "true once a screenshot is uploaded. Fetch it from "
                + "/api/plans/enrollments/{id}/screenshot - it is not inlined here.")
        boolean hasScreenshot,

        String rejectionReason,

        LocalDateTime createdAt,
        LocalDateTime submittedAt,
        LocalDateTime reviewedAt,

        @Schema(description = "Which admin reviewed it")
        String reviewedBy,

        LocalDateTime expiresAt,

        // Context for the admin queue, so it needs no second call.
        @Schema(example = "Rahul Sharma")
        String studentName,

        @Schema(example = "rahul@example.com")
        String studentEmail
) {
    public static PlanEnrollmentVo from(PlanEnrollment e) {
        return new PlanEnrollmentVo(
                e.getId(),
                e.getPlan().getId(),
                e.getPlan().getName(),
                e.getPricePaid(),
                e.getStatus(),
                e.isCurrentlyActive(),
                e.getUpiReference(),
                e.getScreenshotFile() != null,
                e.getRejectionReason(),
                e.getCreatedAt(),
                e.getSubmittedAt(),
                e.getReviewedAt(),
                e.getReviewedBy() == null ? null : e.getReviewedBy().getFullName(),
                e.getExpiresAt(),
                e.getStudent().getFullName(),
                e.getStudent().getEmail()
        );
    }
}
