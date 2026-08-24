package com.learn.interviewmentor.vo.payroll;

import com.learn.interviewmentor.model.MentorPayout;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** One payout, as the admin screen shows it. */
@Schema(description = "A payment to a mentor covering a fixed set of sessions.")
public record MentorPayoutVo(

        Long id,
        Long mentorId,
        @Schema(example = "Ananya Rao") String mentorName,
        @Schema(example = "ananya@example.com") String mentorEmail,

        @Schema(example = "7") int interviewCount,
        @Schema(example = "3") int mentoringCount,
        @Schema(description = "interviewCount + mentoringCount", example = "10") int totalSessions,

        @Schema(description = "The rate used, frozen when this was raised - not the mentor's "
                + "rate today.", example = "800.00")
        BigDecimal interviewRate,

        @Schema(example = "500.00") BigDecimal mentoringRate,
        @Schema(example = "7100.00") BigDecimal amount,

        @Schema(description = "Earliest session covered") LocalDateTime periodStart,
        @Schema(description = "Latest session covered") LocalDateTime periodEnd,

        @Schema(example = "PENDING") String status,
        @Schema(description = "UTR or NEFT reference", example = "N123456789012345") String paymentReference,
        String notes,

        LocalDateTime createdAt,
        @Schema(description = "Which admin raised it") String createdByName,
        LocalDateTime paidAt,
        @Schema(description = "Which admin paid it") String paidByName,
        LocalDateTime cancelledAt,
        String cancelledReason
) {

    public static MentorPayoutVo from(MentorPayout p) {
        return new MentorPayoutVo(
                p.getId(),
                p.getMentor().getId(),
                p.getMentor().getFullName(),
                p.getMentor().getEmail(),
                p.getInterviewCount(),
                p.getMentoringCount(),
                p.totalSessions(),
                p.getInterviewRate(),
                p.getMentoringRate(),
                p.getAmount(),
                p.getPeriodStart(),
                p.getPeriodEnd(),
                p.getStatus().name(),
                p.getPaymentReference(),
                p.getNotes(),
                p.getCreatedAt(),
                p.getCreatedBy() == null ? null : p.getCreatedBy().getFullName(),
                p.getPaidAt(),
                p.getPaidBy() == null ? null : p.getPaidBy().getFullName(),
                p.getCancelledAt(),
                p.getCancelledReason());
    }
}
