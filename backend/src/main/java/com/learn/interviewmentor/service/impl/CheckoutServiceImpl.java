package com.learn.interviewmentor.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.learn.interviewmentor.dto.checkout.CheckoutCallbackDto;
import com.learn.interviewmentor.exception.BadRequestException;
import com.learn.interviewmentor.exception.ForbiddenException;
import com.learn.interviewmentor.exception.NotFoundException;
import com.learn.interviewmentor.exception.PaymentGatewayException;
import com.learn.interviewmentor.model.PaymentIntent;
import com.learn.interviewmentor.model.PaymentIntentStatus;
import com.learn.interviewmentor.model.PaymentPurpose;
import com.learn.interviewmentor.model.User;
import com.learn.interviewmentor.model.WebhookEvent;
import com.learn.interviewmentor.payment.PaymentGateway;
import com.learn.interviewmentor.payment.PurchaseSettlement;
import com.learn.interviewmentor.repository.PaymentIntentRepository;
import com.learn.interviewmentor.repository.WebhookEventRepository;
import com.learn.interviewmentor.service.CheckoutService;
import com.learn.interviewmentor.vo.checkout.CheckoutOptionsVo;
import com.learn.interviewmentor.vo.checkout.CheckoutResultVo;
import com.learn.interviewmentor.vo.checkout.CheckoutVo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * The gateway payment flow, end to end.
 *
 * Read {@link CheckoutService} first for what the three settlement routes are.
 * This class is about making sure they add up to <i>exactly one</i> activation
 * per payment, no matter what order they arrive in or how many times.
 */
@Service
@Transactional(readOnly = true)
public class CheckoutServiceImpl implements CheckoutService {

    private static final Logger log = LoggerFactory.getLogger(CheckoutServiceImpl.class);

    /**
     * How long a checkout can sit open before the sweep gives up on it.
     *
     * Generously long. A UPI collect request can legitimately stay pending for
     * around 15 minutes while the student finds their phone, and netbanking
     * through a slow bank page is worse. Marking one abandoned does no harm - a
     * late webhook still settles it - but a number that fires while people are
     * still paying would make the admin's screen actively misleading.
     */
    private static final int ABANDON_AFTER_HOURS = 6;

    private final PaymentGateway gateway;
    private final PaymentIntentRepository intentRepository;
    private final WebhookEventRepository webhookRepository;
    private final ObjectMapper json;

    /**
     * purpose -> the service that prices and activates that kind of purchase.
     *
     * Built once from whatever implements PurchaseSettlement. Adding a fourth
     * thing to sell means implementing that interface and nothing here changes.
     */
    private final Map<PaymentPurpose, PurchaseSettlement> settlements =
            new EnumMap<>(PaymentPurpose.class);

    public CheckoutServiceImpl(PaymentGateway gateway,
                               PaymentIntentRepository intentRepository,
                               WebhookEventRepository webhookRepository,
                               ObjectMapper json,
                               List<PurchaseSettlement> handlers) {
        this.gateway = gateway;
        this.intentRepository = intentRepository;
        this.webhookRepository = webhookRepository;
        this.json = json;

        for (PurchaseSettlement handler : handlers) {
            PurchaseSettlement clash = settlements.put(handler.purpose(), handler);
            if (clash != null) {
                // Fail at startup, not at settlement time. Two handlers for one
                // purpose means half the payments of that kind activate the
                // wrong thing, and which half depends on bean ordering - the
                // sort of bug that only shows up in production and looks random.
                throw new IllegalStateException(
                        "Two PurchaseSettlement beans claim " + handler.purpose() + ": "
                                + clash.getClass().getSimpleName() + " and "
                                + handler.getClass().getSimpleName());
            }
        }

        // Not an error. Manual UPI reports ready=false because it has no
        // checkout to offer, and everything below simply declines to open one.
        log.info("Checkout: provider={}, gateway checkout={}, handlers={}",
                gateway.name(), gateway.isReady() ? "on" : "off", settlements.keySet());
    }

    @Override
    public CheckoutOptionsVo options() {
        return new CheckoutOptionsVo(gateway.name(), gateway.isReady(), true);
    }

    // ---------- opening a checkout ----------

    @Transactional
    @Override
    public CheckoutVo start(PaymentPurpose purpose, Long targetId, User caller) {
        if (!gateway.isReady()) {
            throw new PaymentGatewayException(
                    "Card and UPI checkout is not switched on for this server");
        }

        PurchaseSettlement handler = handlerFor(purpose);

        // Ownership, state and price - all decided by the service that owns the
        // thing being bought, and all before an order exists at the gateway.
        PurchaseSettlement.Payable payable = handler.prepare(targetId, caller);

        // Already paid for through some other route. Without this check a
        // student who paid by manual UPI could still open a card checkout from a
        // stale tab and be charged a second time for the same booking.
        if (intentRepository.existsByPurposeAndTargetIdAndStatus(
                purpose, targetId, PaymentIntentStatus.PAID)) {
            throw new BadRequestException("This has already been paid for.");
        }

        PaymentIntent intent = reusableIntent(purpose, targetId, caller, payable.amount())
                .orElseGet(() -> intentRepository.save(
                        new PaymentIntent(purpose, targetId, caller, payable.amount())));

        // Written before the gateway is called, so the row exists no matter what
        // happens next. The reference is what comes back on the webhook.
        String reference = reference(purpose, targetId, intent.getId());

        PaymentGateway.Order order = gateway.createOrder(reference, payable.amount(), payable.description());
        intent.attachOrder(order.orderId());

        log.info("Checkout opened: {} for {} ({}) - order {}",
                reference, caller.getEmail(), payable.amount(), order.orderId());

        return new CheckoutVo(
                gateway.name(),
                order.orderId(),
                order.keyId(),
                order.amountInMinorUnits(),
                payable.amount(),
                order.currency(),
                payable.description(),
                caller.getFullName(),
                caller.getEmail());
    }

    /**
     * An open order for this exact thing at this exact price, if there is one.
     *
     * A student who closes the checkout and clicks Pay again should land back on
     * the same order rather than leaving a trail of them. The price is part of
     * the match on purpose: if an admin changed it in between, the old order is
     * for the wrong amount and reusing it would charge yesterday's number.
     */
    private Optional<PaymentIntent> reusableIntent(PaymentPurpose purpose, Long targetId,
                                                   User caller, BigDecimal amount) {
        return intentRepository
                .findByTargetAndStatus(purpose, targetId, PaymentIntentStatus.CREATED)
                .stream()
                .filter(i -> i.getGatewayOrderId() != null)
                .filter(i -> i.isOwnedBy(caller))
                .filter(i -> i.getAmount().compareTo(amount) == 0)
                .filter(i -> i.getCreatedAt().isAfter(LocalDateTime.now().minusHours(ABANDON_AFTER_HOURS)))
                .findFirst();
    }

    // ---------- the browser coming back ----------

    @Transactional
    @Override
    public CheckoutResultVo confirm(CheckoutCallbackDto callback, User caller) {
        if (!gateway.verifyCallbackSignature(
                callback.razorpayOrderId(), callback.razorpayPaymentId(), callback.razorpaySignature())) {
            // Someone posted a payment id the gateway never signed. Logged as a
            // warning because the only ways to get here are a broken integration
            // or an attempt to mark an order paid without paying.
            log.warn("Rejected a checkout callback with a bad signature: order={} payment={} caller={}",
                    callback.razorpayOrderId(), callback.razorpayPaymentId(), caller.getEmail());
            throw new ForbiddenException("That payment could not be verified.");
        }

        PaymentIntent intent = intentRepository
                .findByOrderIdForUpdate(callback.razorpayOrderId())
                .orElseThrow(() -> new NotFoundException("We have no record of that order."));

        // The signature proves the gateway signed this pair. It says nothing
        // about who is asking - and without this check any logged-in student who
        // saw an order id could settle somebody else's payment.
        if (!intent.isOwnedBy(caller)) {
            log.warn("{} tried to confirm order {}, which belongs to somebody else",
                    caller.getEmail(), callback.razorpayOrderId());
            throw new ForbiddenException("That isn't your payment.");
        }

        if (intent.isPaid()) {
            // The webhook beat the browser here. Perfectly normal on a fast
            // connection, and the right answer is the good news, not an error.
            return CheckoutResultVo.settled("Payment received. You're all set.");
        }

        activate(intent, callback.razorpayPaymentId(), "callback");
        return CheckoutResultVo.settled("Payment received. You're all set.");
    }

    // ---------- the webhook ----------

    /**
     * @implNote One transaction covering the event insert and the settlement.
     *
     * A duplicate delivery hits the unique constraint on {@code event_id} and
     * rolls the whole thing back, which is exactly right: nothing was settled
     * twice, and the caller catches the constraint violation and answers 200.
     *
     * Doing it the other way round - check for the row, then insert - reads more
     * naturally and is broken. Two retries arriving at the same moment both find
     * no row, both proceed, and access is granted twice. The constraint is the
     * only thing that holds under concurrency.
     */
    @Transactional
    @Override
    public String handleWebhook(String rawBody, String signature, String eventId) {
        if (!gateway.verifyWebhookSignature(rawBody, signature)) {
            log.warn("Rejected a webhook with a bad signature (event {})", eventId);
            throw new ForbiddenException("Invalid signature");
        }

        JsonNode body;
        try {
            body = json.readTree(rawBody);
        } catch (Exception e) {
            log.error("Webhook {} passed its signature check but is not JSON", eventId, e);
            throw new BadRequestException("Unreadable webhook body");
        }

        String eventType = body.path("event").asText("");
        JsonNode payment = body.path("payload").path("payment").path("entity");
        JsonNode order = body.path("payload").path("order").path("entity");

        String orderId = firstNonBlank(
                payment.path("order_id").asText(null),
                order.path("id").asText(null));

        WebhookEvent record = new WebhookEvent(
                // Razorpay always sends X-Razorpay-Event-Id. Falling back to the
                // order id keeps a gateway that does not from losing idempotency
                // altogether - it is coarser, but it is not nothing.
                firstNonBlank(eventId, orderId + ":" + eventType),
                eventType, orderId, rawBody);

        // Flushed now rather than at commit, so a duplicate is discovered before
        // any settlement work happens instead of after it.
        webhookRepository.saveAndFlush(record);

        String outcome = process(eventType, orderId, payment);
        record.recordOutcome(outcome);

        log.info("Webhook {} [{}] order={} -> {}", eventId, eventType, orderId, outcome);
        return outcome;
    }

    private String process(String eventType, String orderId, JsonNode payment) {
        // Everything else Razorpay sends - refunds, settlements, disputes,
        // subscription events - is recorded and ignored. Answering 200 to an
        // event we do not act on is correct: a non-2xx makes the gateway retry
        // it for a day, and none of those events has anything to activate.
        boolean paid = "order.paid".equals(eventType) || "payment.captured".equals(eventType);
        boolean failed = "payment.failed".equals(eventType);

        if (!paid && !failed) {
            return "ignored - nothing to do for " + eventType;
        }
        if (orderId == null || orderId.isBlank()) {
            return "ignored - no order id in the payload";
        }

        Optional<PaymentIntent> found = intentRepository.findByOrderIdForUpdate(orderId);
        if (found.isEmpty()) {
            // Not an error, and not something to retry. Most often it is a
            // second app pointed at the same Razorpay account, or an order made
            // by hand in their dashboard. Retrying would never find it.
            return "ignored - no intent for order " + orderId;
        }
        PaymentIntent intent = found.get();

        // Checked before either branch, because a paid intent is the answer to
        // both of them. Razorpay's events are not ordered: a payment.failed for
        // an earlier declined card routinely lands after the order.paid for the
        // one that worked.
        if (intent.isPaid()) {
            return failed
                    // PaymentIntent.fail() already refuses to touch a paid
                    // intent, so nothing would have broken either way. The
                    // reason to check here as well is the sentence this
                    // returns: it goes in the webhook log, and that log is what
                    // gets read when a payment is disputed. "Marked failed" next
                    // to an order that was paid is exactly the wrong thing to
                    // find there.
                    ? "ignored - a late failure for a payment that already succeeded"
                    : "already settled via " + intent.getSettledVia();
        }

        if (failed) {
            String reason = firstNonBlank(
                    payment.path("error_description").asText(null),
                    "The payment did not go through.");
            intent.fail(reason);
            return "marked failed: " + reason;
        }

        // What we asked for, against what was actually captured. The gateway is
        // the source of truth for the second number, and a mismatch means the
        // amount was altered somewhere in between - so it is refused rather than
        // activated, and left for a person to look at.
        long expected = intent.getAmount().movePointRight(2).longValueExact();
        long captured = payment.path("amount").asLong(expected);
        if (captured != expected) {
            log.error("Amount mismatch on order {}: asked {} paise, captured {} paise. "
                            + "NOT activating - check this by hand.",
                    orderId, expected, captured);
            return "refused - amount mismatch (expected " + expected + ", got " + captured + ")";
        }

        String gatewayPaymentId = payment.path("id").asText(orderId);
        activate(intent, gatewayPaymentId, "webhook");
        return "settled " + intent.getPurpose() + ":" + intent.getTargetId();
    }

    /**
     * Mark the intent paid and switch on what it bought.
     *
     * The order matters. {@code settle} is the guard: it returns true only for
     * the call that actually changed CREATED to PAID, and the row is locked, so
     * whichever of the callback and the webhook arrives second gets false and
     * activates nothing. Activating first and marking second would leave a
     * window where both routes are inside the same activation.
     */
    private void activate(PaymentIntent intent, String gatewayPaymentId, String via) {
        if (!intent.settle(gatewayPaymentId, via)) {
            return;
        }
        handlerFor(intent.getPurpose()).settle(intent.getTargetId(), gatewayPaymentId);
    }

    private PurchaseSettlement handlerFor(PaymentPurpose purpose) {
        PurchaseSettlement handler = settlements.get(purpose);
        if (handler == null) {
            throw new IllegalStateException("Nothing knows how to settle " + purpose);
        }
        return handler;
    }

    // ---------- housekeeping ----------

    @Transactional
    @Override
    public int sweepAbandoned() {
        List<PaymentIntent> stale = intentRepository.findByStatusAndCreatedAtBefore(
                PaymentIntentStatus.CREATED, LocalDateTime.now().minusHours(ABANDON_AFTER_HOURS));
        stale.forEach(PaymentIntent::abandon);
        return stale.size();
    }

    /**
     * "PLAN:14:207" - purpose, the row it pays for, and the attempt.
     *
     * Goes to the gateway as the order receipt and comes back untouched on the
     * webhook. The intent id on the end makes it unique per attempt, so two
     * tries at the same purchase are distinguishable in their dashboard - which
     * is where you look when somebody says they were charged twice.
     */
    private static String reference(PaymentPurpose purpose, Long targetId, Long intentId) {
        return purpose.name() + ":" + targetId + ":" + intentId;
    }

    private static String firstNonBlank(String a, String b) {
        return a != null && !a.isBlank() ? a : b;
    }
}
