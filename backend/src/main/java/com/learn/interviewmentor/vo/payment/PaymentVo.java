package com.learn.interviewmentor.vo.payment;

import com.learn.interviewmentor.model.Payment;
import com.learn.interviewmentor.model.PaymentStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** A payment as shown to the student who made it, and to the admin checking it. */
@Schema(description = "A manual UPI payment awaiting or having had human verification.")
public record PaymentVo(

        Long id,

        @Schema(description = "Which interview request this paid for", example = "12")
        Long requestId,

        BigDecimal amount,

        @Schema(description = "UPI transaction / UTR number the student entered",
                example = "412345678901")
        String upiReference,

        @Schema(description = "AWAITING, SUBMITTED, VERIFIED or REJECTED", example = "SUBMITTED")
        PaymentStatus status,

        @Schema(description = "Why an admin rejected it, if they did")
        String rejectionReason,

        @Schema(description = "true once a screenshot has been uploaded. Fetch the image from "
                + "/api/payments/{id}/screenshot - it is not inlined here.")
        boolean hasScreenshot,

        LocalDateTime submittedAt,
        LocalDateTime reviewedAt,

        @Schema(description = "Which admin reviewed it")
        String reviewedBy,

        // Handy context for the admin queue, so it doesn't need a second call.
        @Schema(description = "Who is paying", example = "Rahul Sharma")
        String studentName,

        @Schema(description = "What they booked", example = "Spring Boot backend round")
        String topic,

        @Schema(description = "The slot they booked", example = "2026-09-20T15:00:00")
        LocalDateTime slot
) {
    public static PaymentVo from(Payment p) {
        return new PaymentVo(
                p.getId(),
                p.getRequest().getId(),
                p.getAmount(),
                p.getUpiReference(),
                p.getStatus(),
                p.getRejectionReason(),
                p.getScreenshotFile() != null,
                p.getSubmittedAt(),
                p.getReviewedAt(),
                p.getReviewedBy() == null ? null : p.getReviewedBy().getFullName(),
                p.getRequest().getStudent().getFullName(),
                p.getRequest().getTopic(),
                p.getRequest().getPreferredSlot()
        );
    }
}
