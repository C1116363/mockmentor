package com.learn.interviewmentor.model;

/**
 * CREATED  -> order opened at the gateway, student is at the checkout
 * PAID     -> money captured and the thing it bought has been activated
 * FAILED   -> the gateway told us the payment failed
 * ABANDONED-> nobody ever came back. Set by a sweep, not by a student.
 *
 * <h2>There is no CANCELLED</h2>
 * Closing the checkout window sends us nothing at all - the browser simply
 * stops talking to us. An intent that looks abandoned may be a payment still
 * settling at the bank, which is why the sweep waits rather than deciding
 * quickly, and why ABANDONED never blocks a later webhook from settling it.
 */
public enum PaymentIntentStatus {
    CREATED,
    PAID,
    FAILED,
    ABANDONED
}
