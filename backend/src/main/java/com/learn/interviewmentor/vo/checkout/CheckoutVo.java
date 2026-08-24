package com.learn.interviewmentor.vo.checkout;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

/**
 * Everything the browser needs to open a gateway checkout.
 *
 * <h2>What is deliberately not here</h2>
 * No key secret, and no webhook secret. {@code keyId} is publishable by design -
 * the checkout script cannot run without it - but the other two never leave the
 * server, and the easiest way to guarantee that is for the response object to
 * have nowhere to put them.
 */
@Schema(description = "An open gateway order, ready for the checkout window.")
public record CheckoutVo(

        @Schema(description = "Which gateway is taking this payment", example = "Razorpay")
        String provider,

        @Schema(description = "The gateway's order id. Comes back on the webhook.",
                example = "order_QK3nR8xLmPqW2z")
        String orderId,

        @Schema(description = "Publishable key. Safe in a browser - it is what identifies "
                + "the merchant to the checkout script.",
                example = "rzp_test_1DP5mmOlF5G5ag")
        String keyId,

        @Schema(description = "Amount in paise. Gateways work in the smallest unit so money "
                + "never passes through a float.", example = "49900")
        long amountInMinorUnits,

        @Schema(description = "The same amount in rupees, for display", example = "499.00")
        BigDecimal amount,

        @Schema(description = "Currency", example = "INR")
        String currency,

        @Schema(description = "What the student is buying, shown in the checkout window",
                example = "Mock interview with a ConfirmPlacement mentor")
        String description,

        @Schema(description = "Prefilled in the checkout so the student is not retyping "
                + "what we already know", example = "Rahul Sharma")
        String studentName,

        @Schema(example = "rahul@example.com")
        String studentEmail
) {
}
