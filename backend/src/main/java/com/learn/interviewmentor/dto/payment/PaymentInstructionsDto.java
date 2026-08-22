package com.learn.interviewmentor.dto.payment;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

/** What the "pay to book" popup needs to render. */
@Schema(description = "Where to send the money, and how much.")
public record PaymentInstructionsDto(

        @Schema(description = "UPI ID to pay", example = "mockmentor@okhdfcbank")
        String upiId,

        @Schema(description = "Name shown in the UPI app", example = "MockMentor")
        String payeeName,

        @Schema(description = "Fee for one interview", example = "499.00")
        BigDecimal amount,

        @Schema(description = "Currency", example = "INR")
        String currency,

        @Schema(description = "upi:// deep link. On a phone this opens GPay / PhonePe / Paytm "
                + "with the amount already filled in.",
                example = "upi://pay?pa=mockmentor@okhdfcbank&pn=MockMentor&am=499.00&cu=INR")
        String upiDeepLink
) {
}
