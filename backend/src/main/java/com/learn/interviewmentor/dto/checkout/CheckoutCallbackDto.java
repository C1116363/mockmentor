package com.learn.interviewmentor.dto.checkout;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * What Razorpay's checkout hands back to the browser when a payment succeeds.
 *
 * <h2>Every field here is attacker-controlled</h2>
 * This arrives from a browser, so all three values are whatever the client
 * chose to send. Only the signature makes them mean anything: it is an HMAC over
 * {@code orderId|paymentId} that only someone holding the key secret could
 * produce. Trusting the payment id without checking it against the signature
 * would let anyone mark any order paid by posting a made-up id - which is the
 * classic way these integrations are broken.
 *
 * <b>There is no amount field, and there must never be one.</b> The amount is
 * on the PaymentIntent, written before the student ever reached the checkout.
 */
@Schema(description = "The signed result Razorpay's checkout returns to the browser.")
public record CheckoutCallbackDto(

        @NotBlank(message = "Order id is required")
        @Size(max = 80)
        @Schema(example = "order_QK3nR8xLmPqW2z")
        String razorpayOrderId,

        @NotBlank(message = "Payment id is required")
        @Size(max = 80)
        @Schema(example = "pay_QK3nS1yTvBcD4e")
        String razorpayPaymentId,

        @NotBlank(message = "Signature is required")
        @Size(max = 256)
        @Schema(description = "HMAC-SHA256 of \"orderId|paymentId\", keyed with the key secret",
                example = "9ef4dffbfd84f1318f6739a3ce19f9d85851857ae648f114332d8401e0949a3d")
        String razorpaySignature
) {
}
