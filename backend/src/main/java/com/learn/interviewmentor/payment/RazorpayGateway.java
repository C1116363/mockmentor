package com.learn.interviewmentor.payment;

import com.fasterxml.jackson.databind.JsonNode;
import com.learn.interviewmentor.exception.PaymentGatewayException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Map;

/**
 * Razorpay.
 *
 * <h2>No SDK</h2>
 * The official client is one more dependency, one more thing to keep current,
 * and a wrapper over two HTTP calls and an HMAC. Everything used here - RestClient,
 * Jackson, javax.crypto - is already on the classpath. If this grows past order
 * creation and signature checks, revisit; today it does not earn a dependency.
 *
 * <h2>The three secrets</h2>
 * <ul>
 *   <li><b>Key ID</b> ({@code rzp_test_...}) - publishable. It is sent to the
 *       browser by design; the checkout cannot open without it.</li>
 *   <li><b>Key Secret</b> - authenticates our API calls and signs the browser
 *       callback. Server only, forever.</li>
 *   <li><b>Webhook Secret</b> - a <i>different</i> secret, set by you in the
 *       Razorpay dashboard, that signs webhook bodies. People routinely try the
 *       Key Secret here and get a stream of rejected webhooks that looks like an
 *       attack.</li>
 * </ul>
 */
@Component
@ConditionalOnProperty(name = "app.payment.provider", havingValue = "razorpay")
public class RazorpayGateway implements PaymentGateway {

    private static final Logger log = LoggerFactory.getLogger(RazorpayGateway.class);

    private static final String ORDERS_URL = "https://api.razorpay.com/v1/orders";

    private final String keyId;
    private final String keySecret;
    private final String webhookSecret;
    private final RestClient http;

    public RazorpayGateway(@Value("${app.payment.razorpay.key-id:}") String keyId,
                           @Value("${app.payment.razorpay.key-secret:}") String keySecret,
                           @Value("${app.payment.razorpay.webhook-secret:}") String webhookSecret,
                           RestClient.Builder builder) {
        this.keyId = keyId.trim();
        this.keySecret = keySecret.trim();
        this.webhookSecret = webhookSecret.trim();
        this.http = builder.build();

        if (!isReady()) {
            // Warn, do not throw. Refusing to start would mean a missing key
            // takes down interviews, plans, study material and login as well -
            // everything except the one feature that needs it. The payment
            // screens check isReady() and offer manual UPI instead.
            log.warn("app.payment.provider=razorpay but the keys are missing. "
                    + "Card and UPI checkout is switched off and the app will offer manual UPI. "
                    + "Set RAZORPAY_KEY_ID, RAZORPAY_KEY_SECRET and RAZORPAY_WEBHOOK_SECRET in backend/.env");
        } else if (keyId.startsWith("rzp_test_")) {
            log.warn("Razorpay is in TEST mode - no real money will move. "
                    + "Swap in the live keys when you are ready to charge.");
        }
    }

    @Override
    public String name() {
        return "Razorpay";
    }

    /**
     * All three secrets, or none of it works.
     *
     * The webhook secret is included deliberately. Without it we could still
     * open a checkout and take money, but no webhook would ever be trusted - so
     * a student's payment would succeed at the bank and never activate here.
     * Failing at the "Pay" button is far better than failing after payment.
     */
    @Override
    public boolean isReady() {
        return !keyId.isBlank() && !keySecret.isBlank() && !webhookSecret.isBlank();
    }

    @Override
    public Order createOrder(String reference, BigDecimal amount, String description) {
        if (!isReady()) {
            throw new PaymentGatewayException("Razorpay is not configured");
        }

        long paise = toMinorUnits(amount);

        // receipt is ours and comes back on the webhook untouched: it is how a
        // payment is matched to the row it paid for. notes is a free-form map
        // Razorpay echoes back, and a second copy of the same fact costs nothing
        // when the alternative is a payment we cannot attribute.
        Map<String, Object> body = Map.of(
                "amount", paise,
                "currency", "INR",
                "receipt", reference,
                // Capture at authorisation. The alternative is a two-step
                // auth-then-capture, where forgetting the second step means the
                // money is held on the card and never actually taken.
                "payment_capture", 1,
                "notes", Map.of("reference", reference, "description", description));

        JsonNode created;
        try {
            created = http.post()
                    .uri(ORDERS_URL)
                    .header(HttpHeaders.AUTHORIZATION, basicAuth())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(JsonNode.class);
        } catch (Exception e) {
            // The message can carry the request we sent. Log it, do not return
            // it - and never let it near a 5xx body.
            log.error("Razorpay order creation failed for reference {}", reference, e);
            throw new PaymentGatewayException("Could not reach the payment gateway", e);
        }

        if (created == null || !created.hasNonNull("id")) {
            log.error("Razorpay accepted the order for {} but returned no id: {}", reference, created);
            throw new PaymentGatewayException("The payment gateway returned an unusable response");
        }

        return new Order(created.get("id").asText(), keyId, paise, "INR", description);
    }

    /**
     * Webhook signature: HMAC-SHA256 of the raw body, hex, in X-Razorpay-Signature.
     *
     * <b>Raw body.</b> Not a re-serialised object. Jackson will happily give you
     * back JSON that means the same thing with different whitespace and key
     * order, and the HMAC of that is a different HMAC. This is the single most
     * common reason a correct-looking integration rejects every webhook.
     */
    @Override
    public boolean verifyWebhookSignature(String rawBody, String signature) {
        if (!isReady() || rawBody == null || signature == null || signature.isBlank()) {
            return false;
        }
        return constantTimeEquals(hmacHex(webhookSecret, rawBody), signature.trim());
    }

    /**
     * Browser callback signature: HMAC-SHA256 of "orderId|paymentId", keyed with
     * the <i>Key Secret</i> - not the webhook secret. Two different messages,
     * two different keys; mixing them up is the other classic failure.
     *
     * <h2>Why this is not enough on its own</h2>
     * A valid signature here proves Razorpay signed this pair, and nothing more.
     * It does not prove the money was captured, and the callback may never
     * arrive at all - the student's phone dies, the tab is closed, the network
     * drops between the bank and the browser. The webhook is what settles a
     * payment. This check exists so the student who is still looking at the
     * screen gets an answer immediately instead of watching a spinner.
     */
    @Override
    public boolean verifyCallbackSignature(String orderId, String paymentId, String signature) {
        if (!isReady() || orderId == null || paymentId == null || signature == null || signature.isBlank()) {
            return false;
        }
        return constantTimeEquals(hmacHex(keySecret, orderId + "|" + paymentId), signature.trim());
    }

    /**
     * Rupees to paise.
     *
     * Gateways work in the smallest unit so that money never passes through a
     * float - 0.1 + 0.2 is famously not 0.3, and a rounding error in a price is
     * a rounding error in somebody's bank account. UNNECESSARY throws rather
     * than rounds: a price with sub-paise precision is a data bug, and quietly
     * charging a different number than the one on screen is worse than an error.
     */
    static long toMinorUnits(BigDecimal rupees) {
        return rupees.setScale(2, RoundingMode.UNNECESSARY)
                .movePointRight(2)
                .longValueExact();
    }

    private String basicAuth() {
        String raw = keyId + ":" + keySecret;
        return "Basic " + Base64.getEncoder().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    static String hmacHex(String secret, String message) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(message.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            // Both causes are "HmacSHA256 is missing from this JVM" or "the key
            // is empty" - configuration, not traffic. Never a reason to accept.
            throw new PaymentGatewayException("Could not compute the payment signature", e);
        }
    }

    /**
     * Compared in constant time.
     *
     * String.equals returns as soon as two bytes differ, so how long it takes
     * leaks how much of a guessed signature was right. That is enough to forge
     * one byte at a time given enough attempts, and a webhook endpoint accepts
     * as many attempts as anyone wants to send.
     */
    static boolean constantTimeEquals(String a, String b) {
        return MessageDigest.isEqual(
                a.getBytes(StandardCharsets.UTF_8),
                b.getBytes(StandardCharsets.UTF_8));
    }
}
