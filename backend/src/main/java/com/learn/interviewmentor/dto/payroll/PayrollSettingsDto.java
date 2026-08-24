package com.learn.interviewmentor.dto.payroll;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

/** Turning payroll on for a mentor, and setting what they earn. */
@Schema(description = "A mentor's payroll settings.")
public record PayrollSettingsDto(

        @Schema(description = "Whether this mentor is on payroll", example = "true")
        boolean enabled,

        /*
         * Nullable so payroll can be switched off without also wiping the rates
         * - somebody stepping away for a month should come back to their old
         * numbers rather than a blank form. The service refuses to enable
         * payroll with either one missing.
         */
        @PositiveOrZero(message = "A rate cannot be negative")
        @Digits(integer = 8, fraction = 2, message = "Use at most two decimal places")
        @DecimalMax(value = "1000000.00", message = "That rate looks like a typo")
        @Schema(description = "Paid per completed mock interview", example = "800.00")
        BigDecimal interviewRate,

        @PositiveOrZero(message = "A rate cannot be negative")
        @Digits(integer = 8, fraction = 2, message = "Use at most two decimal places")
        @DecimalMax(value = "1000000.00", message = "That rate looks like a typo")
        @Schema(description = "Paid per completed mentoring session", example = "500.00")
        BigDecimal mentoringRate
) {
}
