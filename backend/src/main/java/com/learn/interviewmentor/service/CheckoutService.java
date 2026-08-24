package com.learn.interviewmentor.service;

import com.learn.interviewmentor.dto.checkout.CheckoutCallbackDto;
import com.learn.interviewmentor.model.PaymentPurpose;
import com.learn.interviewmentor.model.User;
import com.learn.interviewmentor.vo.checkout.CheckoutOptionsVo;
import com.learn.interviewmentor.vo.checkout.CheckoutResultVo;
import com.learn.interviewmentor.vo.checkout.CheckoutVo;

/**
 * Taking money through a gateway.
 *
 * <h2>The three ways this app learns a payment happened, in order of trust</h2>
 * <ol>
 *   <li><b>The webhook.</b> Gateway to server, signed, retried until we answer.
 *       This is the authority. Everything else is a convenience.</li>
 *   <li><b>The browser callback.</b> Signed, so it is genuine - but it only
 *       arrives if the student's browser survives long enough to send it. Used
 *       so the person staring at the screen gets an answer now.</li>
 *   <li><b>An admin, by hand.</b> The existing manual UPI path, unchanged.</li>
 * </ol>
 *
 * <h2>Why the webhook cannot be skipped</h2>
 * It is tempting to settle on the callback alone - it is simpler, and it works
 * every time you test it, because your laptop does not run out of battery
 * between the bank and the redirect. In production the callback goes missing
 * often enough to matter: closed tabs, dead phones, a network that drops on the
 * way back from a UPI app. Those students have paid. Without a webhook, nothing
 * ever tells us.
 */
public interface CheckoutService {

    /** What the payment screen should offer: gateway, manual UPI, or both. */
    CheckoutOptionsVo options();

    /**
     * Open a gateway order for something the caller is about to pay for.
     *
     * The amount is read from the row named by {@code targetId}, never from the
     * caller. Ownership is checked by the {@code PurchaseSettlement} for this
     * purpose before an order is opened.
     */
    CheckoutVo start(PaymentPurpose purpose, Long targetId, User caller);

    /**
     * The browser reporting a successful payment.
     *
     * Verifies the signature, then settles - unless the webhook got there first,
     * in which case it reports what already happened rather than doing it twice.
     */
    CheckoutResultVo confirm(CheckoutCallbackDto callback, User caller);

    /**
     * A signed webhook from the gateway.
     *
     * @param rawBody   the exact bytes received - not a re-serialised object
     * @param signature the gateway's signature header
     * @param eventId   the gateway's delivery id, used for idempotency
     * @return a short line describing what was done, for the log
     * @throws com.learn.interviewmentor.exception.ForbiddenException if the
     *         signature does not verify
     */
    String handleWebhook(String rawBody, String signature, String eventId);

    /**
     * Age out checkouts nobody came back from.
     *
     * Purely cosmetic - an ABANDONED intent is never consulted by anything, and
     * a late webhook still settles one. It exists so the admin's view of
     * in-flight payments is not permanently full of orders from last month.
     */
    int sweepAbandoned();
}
