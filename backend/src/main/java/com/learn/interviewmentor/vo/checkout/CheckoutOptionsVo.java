package com.learn.interviewmentor.vo.checkout;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * How this app can be paid, right now.
 *
 * <h2>Why the frontend asks instead of being told at build time</h2>
 * Which gateway is configured is a property on the server, and the keys can be
 * absent even when a gateway is selected. Baking the answer into the frontend
 * would mean a rebuild to switch payment method, and - worse - a payment button
 * that is present because someone intended to configure a gateway rather than
 * because they actually did.
 */
@Schema(description = "Which payment methods this server can offer.")
public record CheckoutOptionsVo(

        @Schema(description = "Name of the configured gateway", example = "Razorpay")
        String provider,

        @Schema(description = "Whether card / netbanking / UPI checkout can actually run. "
                + "False when a gateway is selected but its keys are missing - the UI then "
                + "offers manual UPI only, rather than a button that cannot work.",
                example = "true")
        boolean gatewayReady,

        @Schema(description = "Whether pay-by-UPI-and-upload-a-screenshot is still offered. "
                + "Kept on alongside a gateway: it costs nothing, and it is the way to take "
                + "money on a day the gateway is down.", example = "true")
        boolean manualUpiAvailable
) {
}
