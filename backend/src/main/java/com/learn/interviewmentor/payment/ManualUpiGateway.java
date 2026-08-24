package com.learn.interviewmentor.payment;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * What this app does today: the student pays a UPI ID from their own app and an
 * admin checks the reference against the bank.
 *
 * The default, and not merely a placeholder. It costs 0% where a gateway costs
 * about 2.4%, the money arrives instantly rather than on T+2, and it needs no
 * KYC - which matters when a gateway account can take a week to approve. Keeping
 * it also means there is a way to take money on the day the gateway is down.
 */
@Component
@ConditionalOnProperty(name = "app.payment.provider", havingValue = "manual", matchIfMissing = true)
public class ManualUpiGateway implements PaymentGateway {

    @Override
    public String name() {
        return "Manual UPI";
    }

    /**
     * False, and that is not a defect.
     *
     * isReady() answers one question: "can this take a payment through a
     * checkout window right now?" Manual UPI never can - there is no order to
     * create and no window to open, which is why createOrder() below throws.
     *
     * Reading it as "is this provider configured and working" and returning
     * true is the tempting mistake, and it has a concrete cost: the payment
     * screen asks exactly this question to decide whether to show a Pay button,
     * so a true here puts a button on screen whose only possible outcome is a
     * 500. The manual UPI flow is unaffected either way - it is reported
     * separately, and it is always on.
     */
    @Override
    public boolean isReady() {
        return false;
    }

    @Override
    public Order createOrder(String reference, BigDecimal amount, String description) {
        throw new UnsupportedOperationException(
                "Manual UPI has no orders - the student pays the UPI ID and uploads proof. "
                        + "Callers should check isReady() and the provider name before "
                        + "offering a checkout.");
    }

    @Override
    public boolean verifyWebhookSignature(String rawBody, String signature) {
        // Nothing sends us webhooks, so nothing can be verified - and returning
        // true here would make the webhook endpoint accept anything the moment
        // somebody switched provider back to manual.
        return false;
    }

    @Override
    public boolean verifyCallbackSignature(String orderId, String paymentId, String signature) {
        return false;
    }
}
