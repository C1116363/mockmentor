package com.learn.interviewmentor.payment;

import com.learn.interviewmentor.model.PaymentPurpose;
import com.learn.interviewmentor.model.User;

import java.math.BigDecimal;

/**
 * How one kind of purchase is priced and how it is switched on once paid.
 *
 * <h2>Why an interface and not a switch</h2>
 * A webhook knows two things: a purpose and a row id. Turning that into
 * "activate enrollment 14" could be a switch statement in the settlement
 * service - and then the settlement service would need a plan repository, a
 * project repository, the seat-availability rule, the GitHub granter, and the
 * expiry-window arithmetic. Every rule that already lives in a service would
 * have a second, drifting copy next to the payment code.
 *
 * So the direction is inverted. Each service already owns its own rules, and
 * implements this to say "here is my price, and here is what paid means for
 * me". The settlement service holds a map from purpose to implementation and
 * knows nothing else. A fourth thing to sell is a fourth implementation and no
 * edit to the payment code.
 *
 * <h2>Implemented by the existing services</h2>
 * PaymentServiceImpl, PlanEnrollmentServiceImpl and ProjectAccessServiceImpl
 * implement this directly rather than getting adapter classes of their own. An
 * adapter would only be able to reach the same repositories and re-derive the
 * same rules from outside, which is the duplication this interface exists to
 * prevent.
 */
public interface PurchaseSettlement {

    /** Which kind of purchase this handles. Unique across implementations. */
    PaymentPurpose purpose();

    /**
     * Check the caller may pay for this, and say what it costs.
     *
     * Runs before an order is opened at the gateway, and is the only ownership
     * check on the way in - so it must throw rather than return anything
     * falsy if the caller does not own the row, or if the row is in a state
     * where paying makes no sense (already active, cancelled, expired).
     *
     * <b>The amount comes from here, never from the client.</b> This is the
     * method that makes that true.
     */
    Payable prepare(Long targetId, User caller);

    /**
     * Money confirmed - switch the thing on.
     *
     * <h2>Called exactly once per payment, and that is not this method's doing</h2>
     * The caller holds a locked PaymentIntent and only reaches here when that
     * intent moved CREATED to PAID. Implementations therefore do not need their
     * own duplicate guard, but they must still be safe to run twice: a bug
     * upstream should waste an activation, not corrupt a row.
     *
     * <h2>Do not throw for things that are not failures</h2>
     * A throw here rolls back the settlement, and the gateway will redeliver -
     * forever, on a payment that already succeeded. Something like a GitHub
     * invite failing is recorded on the row and left for an admin, exactly as it
     * is when an admin approves by hand. Only a genuinely unrecoverable state
     * (the row is gone) should throw.
     */
    void settle(Long targetId, String gatewayPaymentId);

    /**
     * @param amount      rupees, read from the row
     * @param description what the student sees in the checkout window
     */
    record Payable(BigDecimal amount, String description) {
    }
}
