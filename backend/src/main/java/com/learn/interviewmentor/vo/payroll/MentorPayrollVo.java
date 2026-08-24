package com.learn.interviewmentor.vo.payroll;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** One row of the payroll screen: a mentor, their rates, and what they are owed. */
@Schema(description = "A mentor's payroll standing.")
public record MentorPayrollVo(

        @Schema(description = "The mentor's user id - what payout endpoints take", example = "3")
        Long mentorId,

        @Schema(example = "Ananya Rao") String mentorName,
        @Schema(example = "ananya@example.com") String mentorEmail,

        @Schema(description = "Whether an admin has verified their profile. A mentor who is "
                + "not verified cannot have taken sessions, so they will show zero.",
                example = "VERIFIED")
        String verificationStatus,

        @Schema(description = "On payroll. Off until an admin turns it on, so nobody lands in "
                + "a payout run at a rate nobody chose.", example = "true")
        boolean payrollEnabled,

        @Schema(description = "Paid per completed mock interview. Null until set.", example = "800.00")
        BigDecimal interviewRate,

        @Schema(description = "Paid per completed mentoring session. Null until set.", example = "500.00")
        BigDecimal mentoringRate,

        @Schema(description = "Completed interviews not yet in any payout", example = "7")
        int unpaidInterviews,

        @Schema(description = "Completed mentoring sessions not yet in any payout", example = "3")
        int unpaidMentoring,

        @Schema(description = "What a payout raised right now would come to. Zero until both "
                + "rates are set - the sessions are still counted, they just cannot be priced.",
                example = "7100.00")
        BigDecimal amountDue,

        @Schema(description = "Everything ever actually paid to this mentor", example = "24500.00")
        BigDecimal totalPaid,

        @Schema(description = "True when a payout is already raised and awaiting payment. "
                + "Another cannot be created until it is paid or cancelled.", example = "false")
        boolean hasPendingPayout,

        @Schema(description = "When they were last actually paid. Null if never.")
        LocalDateTime lastPaidAt,

        // ---- bank details, so an admin can pay without leaving the screen ----

        @Schema(example = "Ananya Rao") String bankAccountHolder,
        @Schema(example = "50100123456789") String bankAccountNumber,
        @Schema(example = "HDFC0001234") String bankIfsc,
        @Schema(example = "HDFC Bank") String bankName,
        @Schema(example = "ABCDE1234F") String panNumber,

        @Schema(description = "Whether the four bank fields above are all present. The screen "
                + "uses this to warn before a payout is raised for somebody who cannot be paid.",
                example = "true")
        boolean bankDetailsComplete
) {
}
