package com.learn.interviewmentor.dto.payroll;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Recording that a payout has actually been sent.
 *
 * The reference is required rather than optional on purpose. "Mark as paid"
 * with nothing attached is a checkbox that means only that somebody clicked it;
 * the whole value of this record turns up months later, when a mentor says they
 * were not paid and the bank reference is the thing that settles it.
 */
@Schema(description = "Confirm a payout has been sent.")
public record MarkPaidDto(

        @NotBlank(message = "Enter the bank reference - it is what settles a query later")
        @Size(max = 100)
        @Schema(description = "UTR, NEFT or IMPS reference from your bank",
                example = "N123456789012345")
        String paymentReference,

        @Size(max = 500)
        @Schema(description = "Anything worth remembering about this payment",
                example = "Paid with October's batch")
        String notes
) {
}
