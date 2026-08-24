package com.learn.interviewmentor.vo.payroll;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

/** The one-line state of payroll, for the section header. */
@Schema(description = "Payroll totals across all mentors.")
public record PayrollSummaryVo(

        @Schema(description = "Mentors with payroll switched on", example = "4")
        long mentorsOnPayroll,

        @Schema(description = "Mentors with unpaid completed sessions", example = "3")
        long mentorsWithWorkOwed,

        @Schema(description = "What every mentor is collectively owed right now", example = "31400.00")
        BigDecimal totalOwed,

        @Schema(description = "Payouts raised but not yet paid", example = "2")
        long pendingPayouts,

        @Schema(description = "Value of those pending payouts", example = "12600.00")
        BigDecimal pendingAmount,

        @Schema(description = "Everything ever paid out", example = "184000.00")
        BigDecimal totalPaid
) {
}
