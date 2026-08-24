package com.learn.interviewmentor.payment;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The two things in a payment integration that fail silently.
 *
 * A signature check that always returns true passes every manual test anyone
 * would think to run - money still arrives, purchases still activate - while
 * accepting a forged webhook from anybody who knows the URL. And an amount
 * conversion that is off by a factor of ten is only noticed by whoever gets the
 * bank statement.
 *
 * Neither needs a Razorpay account to test, which is the point: these run today,
 * before any keys exist.
 */
class RazorpayGatewayTest {

    private static final String KEY_ID = "rzp_test_1DP5mmOlF5G5ag";
    private static final String KEY_SECRET = "test_key_secret";
    private static final String WEBHOOK_SECRET = "test_webhook_secret";

    private RazorpayGateway gateway(String keyId, String keySecret, String webhookSecret) {
        return new RazorpayGateway(keyId, keySecret, webhookSecret, RestClient.builder());
    }

    private RazorpayGateway configured() {
        return gateway(KEY_ID, KEY_SECRET, WEBHOOK_SECRET);
    }

    @Nested
    @DisplayName("webhook signatures")
    class WebhookSignatures {

        /**
         * The reference value comes from an independent implementation:
         *
         *   printf '%s' '{"event":"order.paid","payload":{}}' \
         *     | openssl dgst -sha256 -hmac 'test_webhook_secret'
         *
         * Testing our HMAC against our own HMAC would pass even if both were
         * wrong. This pins it to something outside the codebase.
         */
        @Test
        @DisplayName("matches an HMAC computed by openssl")
        void matchesOpenSsl() {
            String body = "{\"event\":\"order.paid\",\"payload\":{}}";
            String expected = "31dd378d1f2c7f0bec9f2714291aa33926f5314d2e4ae3eab2e65a2e2c7a4c14";

            assertEquals(expected, RazorpayGateway.hmacHex(WEBHOOK_SECRET, body));
            assertTrue(configured().verifyWebhookSignature(body, expected));
        }

        /**
         * The whole point of the exercise. If a changed body still verifies,
         * anyone can post their own "you have been paid" and get access free.
         */
        @Test
        @DisplayName("rejects a body that has been edited after signing")
        void rejectsTamperedBody() {
            String body = "{\"event\":\"order.paid\",\"payload\":{}}";
            String signature = RazorpayGateway.hmacHex(WEBHOOK_SECRET, body);

            assertFalse(configured().verifyWebhookSignature(
                    "{\"event\":\"order.paid\",\"payload\":{ }}", signature));
        }

        /** Signed with the wrong key - what a forger without the secret can do. */
        @Test
        @DisplayName("rejects a signature made with a different secret")
        void rejectsWrongSecret() {
            String body = "{\"event\":\"order.paid\"}";
            String forged = RazorpayGateway.hmacHex("not_the_webhook_secret", body);

            assertFalse(configured().verifyWebhookSignature(body, forged));
        }

        /**
         * A missing header must not be read as "nothing to check, so fine".
         * This is the failure mode where an integration accepts every
         * unsigned request while looking like it verifies signatures.
         */
        @Test
        @DisplayName("rejects a missing or empty signature")
        void rejectsMissingSignature() {
            String body = "{\"event\":\"order.paid\"}";

            assertFalse(configured().verifyWebhookSignature(body, null));
            assertFalse(configured().verifyWebhookSignature(body, ""));
            assertFalse(configured().verifyWebhookSignature(body, "   "));
        }

        /**
         * Whitespace around a header value is normal in HTTP and must not turn
         * a genuine webhook into a rejected one.
         */
        @Test
        @DisplayName("tolerates whitespace around the header value")
        void tolerantOfPadding() {
            String body = "{\"event\":\"order.paid\"}";
            String signature = RazorpayGateway.hmacHex(WEBHOOK_SECRET, body);

            assertTrue(configured().verifyWebhookSignature(body, "  " + signature + "  "));
        }

        /**
         * An unconfigured gateway verifies nothing.
         *
         * Without this, switching provider to razorpay before the keys arrive
         * would leave an open endpoint that HMACs against an empty secret - a
         * secret an attacker also knows.
         */
        @Test
        @DisplayName("verifies nothing when the keys are missing")
        void rejectsWhenUnconfigured() {
            RazorpayGateway blank = gateway("", "", "");
            String body = "{\"event\":\"order.paid\"}";

            assertFalse(blank.isReady());
            assertFalse(blank.verifyWebhookSignature(body, "any-signature-at-all"));
            assertFalse(blank.verifyCallbackSignature("order_A", "pay_B", "any-signature-at-all"));
        }

        /**
         * The second line of defence, found while writing the test above.
         *
         * The isReady() guard is what stops an unconfigured gateway verifying
         * anything. But even if that guard were removed, an empty secret cannot
         * produce an HMAC at all - SecretKeySpec refuses it - so the failure is
         * a thrown exception rather than a signature an attacker could also
         * compute. Worth pinning: the day somebody "simplifies" the guard away,
         * this is what stops it becoming an open endpoint.
         */
        @Test
        @DisplayName("cannot even compute an HMAC with an empty secret")
        void emptySecretCannotSign() {
            assertThrows(RuntimeException.class,
                    () -> RazorpayGateway.hmacHex("", "{\"event\":\"order.paid\"}"));
        }
    }

    @Nested
    @DisplayName("browser callback signatures")
    class CallbackSignatures {

        /** Signed over "orderId|paymentId", keyed with the KEY secret. */
        @Test
        @DisplayName("accepts a correctly signed order/payment pair")
        void acceptsGenuinePair() {
            String orderId = "order_QK3nR8xLmPqW2z";
            String paymentId = "pay_QK3nS1yTvBcD4e";
            String signature = RazorpayGateway.hmacHex(KEY_SECRET, orderId + "|" + paymentId);

            assertTrue(configured().verifyCallbackSignature(orderId, paymentId, signature));
        }

        /**
         * Swapping in a different payment id is exactly the attack: "this order
         * was paid, here is a payment id I made up".
         */
        @Test
        @DisplayName("rejects a payment id that was not the one signed")
        void rejectsSubstitutedPaymentId() {
            String orderId = "order_QK3nR8xLmPqW2z";
            String signature = RazorpayGateway.hmacHex(KEY_SECRET, orderId + "|pay_real");

            assertFalse(configured().verifyCallbackSignature(orderId, "pay_invented", signature));
        }

        /**
         * The two secrets are not interchangeable. Signing a callback with the
         * webhook secret must fail - and getting this backwards in the
         * implementation is one of the two classic ways this goes wrong.
         */
        @Test
        @DisplayName("does not accept the webhook secret in place of the key secret")
        void secretsAreNotInterchangeable() {
            String orderId = "order_A";
            String paymentId = "pay_B";
            String wrongKey = RazorpayGateway.hmacHex(WEBHOOK_SECRET, orderId + "|" + paymentId);

            assertFalse(configured().verifyCallbackSignature(orderId, paymentId, wrongKey));
        }
    }

    @Nested
    @DisplayName("rupees to paise")
    class MinorUnits {

        @Test
        @DisplayName("converts whole and fractional rupees")
        void converts() {
            assertEquals(49900, RazorpayGateway.toMinorUnits(new BigDecimal("499.00")));
            assertEquals(49900, RazorpayGateway.toMinorUnits(new BigDecimal("499")));
            assertEquals(1, RazorpayGateway.toMinorUnits(new BigDecimal("0.01")));
            assertEquals(1234567, RazorpayGateway.toMinorUnits(new BigDecimal("12345.67")));
            assertEquals(0, RazorpayGateway.toMinorUnits(BigDecimal.ZERO));
        }

        /**
         * Refuses rather than rounds.
         *
         * A price with sub-paise precision is a data bug, and the two ways to
         * absorb it are both worse than an exception: rounding down charges less
         * than the row says, rounding up charges more than the screen said.
         */
        @Test
        @DisplayName("refuses an amount it cannot represent exactly")
        void refusesSubPaise() {
            assertThrows(ArithmeticException.class,
                    () -> RazorpayGateway.toMinorUnits(new BigDecimal("1.005")));
        }
    }

    @Nested
    @DisplayName("readiness")
    class Readiness {

        /**
         * All three, or nothing. Missing the webhook secret is the dangerous
         * case: checkout would work, money would be taken, and no payment would
         * ever activate because no webhook could be trusted.
         */
        @Test
        @DisplayName("needs all three secrets")
        void needsEverySecret() {
            assertTrue(gateway(KEY_ID, KEY_SECRET, WEBHOOK_SECRET).isReady());

            assertFalse(gateway("", KEY_SECRET, WEBHOOK_SECRET).isReady());
            assertFalse(gateway(KEY_ID, "", WEBHOOK_SECRET).isReady());
            assertFalse(gateway(KEY_ID, KEY_SECRET, "").isReady());
        }

        /** Whitespace in a pasted .env value is not a configured key. */
        @Test
        @DisplayName("treats a whitespace-only key as missing")
        void whitespaceIsNotAKey() {
            assertFalse(gateway("  ", KEY_SECRET, WEBHOOK_SECRET).isReady());
        }

        /**
         * Manual UPI reports no checkout, ever.
         *
         * isReady() is the single question the payment screen asks before
         * putting a Pay button on screen. Manual UPI has no order to create and
         * no window to open, so a true here would render a button whose only
         * possible outcome is a 500 - which is exactly what happened before this
         * test existed.
         */
        @Test
        @DisplayName("manual UPI never offers a checkout")
        void manualOffersNoCheckout() {
            ManualUpiGateway manual = new ManualUpiGateway();

            assertFalse(manual.isReady());
            assertThrows(UnsupportedOperationException.class,
                    () -> manual.createOrder("PLAN:1", new BigDecimal("499.00"), "A plan"));
            // And it verifies nothing, so switching back to manual cannot leave
            // the webhook endpoint accepting whatever it is sent.
            assertFalse(manual.verifyWebhookSignature("{}", "anything"));
            assertFalse(manual.verifyCallbackSignature("order_A", "pay_B", "anything"));
        }
    }

    @Nested
    @DisplayName("constant-time comparison")
    class ConstantTime {

        @Test
        @DisplayName("still compares correctly")
        void comparesCorrectly() {
            assertTrue(RazorpayGateway.constantTimeEquals("abc123", "abc123"));
            assertFalse(RazorpayGateway.constantTimeEquals("abc123", "abc124"));
            // Differing in the first byte and differing in the last must both be
            // false - the timing property is what is untestable here, the
            // correctness is not.
            assertFalse(RazorpayGateway.constantTimeEquals("zbc123", "abc123"));
            assertFalse(RazorpayGateway.constantTimeEquals("abc", "abc123"));
            assertFalse(RazorpayGateway.constantTimeEquals("", "a"));
        }
    }
}
