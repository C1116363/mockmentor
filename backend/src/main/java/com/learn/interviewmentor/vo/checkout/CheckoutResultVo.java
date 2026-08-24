package com.learn.interviewmentor.vo.checkout;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * The answer to "did that payment work?", asked by the browser right after the
 * checkout window closes.
 *
 * <h2>Why {@code settled} can be false on a successful payment</h2>
 * The browser callback and the gateway's webhook race. Usually the callback
 * wins and this comes back settled. Sometimes it does not, and the honest answer
 * is "we have your payment, we are waiting for the gateway to confirm it" -
 * which is what {@code pending} means. It is not a failure and the student
 * should not be invited to pay again.
 */
@Schema(description = "What happened to a payment.")
public record CheckoutResultVo(

        @Schema(description = "True when the purchase is now active", example = "true")
        boolean settled,

        @Schema(description = "True when the money looks taken but confirmation has not "
                + "arrived yet. Not a failure - do not offer to pay again.", example = "false")
        boolean pending,

        @Schema(description = "Plain-English status for the student",
                example = "Payment received. Your booking is in the mentor queue.")
        String message
) {

    public static CheckoutResultVo settled(String message) {
        return new CheckoutResultVo(true, false, message);
    }

    public static CheckoutResultVo pending(String message) {
        return new CheckoutResultVo(false, true, message);
    }
}
