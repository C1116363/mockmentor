package com.learn.interviewmentor.payment;

import java.math.BigDecimal;

/**
 * Taking money, behind an interface.
 *
 * Which gateway you use is a deployment decision, not a business rule - the same
 * reasoning as MeetingLinkGenerator and CollaboratorGranter. Nothing in the
 * service layer knows whether a payment arrives through Razorpay or a human
 * checking a UPI reference against the bank.
 *
 * <h2>The rule the whole design turns on</h2>
 * <b>The amount is never taken from the client.</b> Every implementation reads it
 * from the row being paid for. A checkout that accepts an amount from the browser
 * is a shop where the customer writes their own price tag, and it is the single
 * most common way a payment integration is exploited.
 */
public interface PaymentGateway {

    /** Shown in the admin screen and the payment UI: "Razorpay" or "Manual UPI". */
    String name();

    /**
     * Whether this gateway can actually take money right now.
     *
     * False when it is selected but not configured - no keys yet. The UI falls
     * back to manual rather than opening a checkout that cannot work.
     */
    boolean isReady();

    /**
     * Start a payment.
     *
     * @param reference our own id for what is being paid for, e.g. "PLAN:14".
     *                  Comes back on the webhook, and is how a payment is matched
     *                  to the thing it paid for.
     * @param amount    rupees, read from the server-side row. Never from a client.
     */
    Order createOrder(String reference, BigDecimal amount, String description);

    /**
     * Is this webhook really from the gateway?
     *
     * @param rawBody   the exact bytes received. <b>Not</b> a re-serialised object -
     *                  re-encoding JSON changes whitespace and key order, and the
     *                  signature will not match.
     * @param signature the header the gateway sent
     */
    boolean verifyWebhookSignature(String rawBody, String signature);

    /**
     * Is this browser callback genuine?
     *
     * Separate from the webhook check because the gateway signs them differently -
     * the callback signs "orderId|paymentId", the webhook signs the whole body.
     */
    boolean verifyCallbackSignature(String orderId, String paymentId, String signature);

    /**
     * What a client needs to open the checkout.
     *
     * @param orderId  the gateway's id for this order
     * @param keyId    the publishable key - safe in a browser, unlike the secret
     * @param amountInMinorUnits paise. Gateways work in the smallest unit so that
     *                           no money ever passes through a float.
     */
    record Order(String orderId, String keyId, long amountInMinorUnits,
                 String currency, String description) {
    }
}
