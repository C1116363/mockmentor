package com.learn.interviewmentor.controller;

import com.learn.interviewmentor.exception.ForbiddenException;
import com.learn.interviewmentor.facade.CheckoutFacade;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;

/**
 * Where the gateway tells us money moved.
 *
 * <h2>This endpoint is unauthenticated, and that is not a mistake</h2>
 * Razorpay's servers have no login here and never will. What replaces the login
 * is the signature: an HMAC over the body, keyed with a secret only they and we
 * hold. A request without a valid one gets a 403 and touches nothing.
 *
 * <h2>Two things that quietly break this and are easy to get wrong</h2>
 * <ul>
 *   <li><b>Reading the body as an object.</b> The signature is over the exact
 *       bytes sent. Bind to a DTO and re-serialise it and the JSON is equivalent
 *       but not identical - different whitespace, different key order - and the
 *       HMAC no longer matches. Hence {@code byte[]}.</li>
 *   <li><b>CSRF.</b> If CSRF protection is on, this POST is rejected before the
 *       controller runs, and the symptom is a stream of 403s that look like
 *       signature failures. It is already off for this API because the app
 *       authenticates with a bearer token rather than a cookie, but it is worth
 *       knowing where the bodies are buried.</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/webhooks")
@Tag(name = "5. Checkout (gateway)")
public class PaymentWebhookController {

    private static final Logger log = LoggerFactory.getLogger(PaymentWebhookController.class);

    private final CheckoutFacade checkoutFacade;

    public PaymentWebhookController(CheckoutFacade checkoutFacade) {
        this.checkoutFacade = checkoutFacade;
    }

    @PostMapping("/razorpay")
    @Operation(
            summary = "Razorpay webhook (not called by the frontend)",
            description = """
                    Razorpay posts here when a payment succeeds or fails. This - not the
                    browser - is what actually activates a purchase.

                    **Set it up in the Razorpay dashboard** under Settings → Webhooks:
                    the URL is `https://your-domain/api/webhooks/razorpay`, the events are
                    `order.paid`, `payment.captured` and `payment.failed`, and the secret you
                    choose there goes in `RAZORPAY_WEBHOOK_SECRET`. It is a *different*
                    secret from the API key secret; using the key secret here produces a
                    stream of rejected webhooks that looks like an attack.

                    Localhost is not reachable from Razorpay, so for development put a tunnel
                    in front of it: `cloudflared tunnel --url http://localhost:8080`.

                    Always answers 200 for anything it accepted, including deliveries it has
                    already seen and events it does not act on. Razorpay retries on any
                    non-2xx, so returning an error for a duplicate would keep it retrying a
                    webhook that has already done its job.
                    """)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Accepted"),
            @ApiResponse(responseCode = "403", description = "Signature did not verify")
    })
    public ResponseEntity<String> razorpay(
            // byte[], not String and not a DTO. The signature is over the exact
            // bytes; anything that decodes and re-encodes them can change what
            // is hashed. Decoded here as UTF-8, which is what Razorpay sends and
            // what the HMAC must be computed over.
            @RequestBody byte[] rawBody,
            @RequestHeader(value = "X-Razorpay-Signature", required = false) String signature,
            @RequestHeader(value = "X-Razorpay-Event-Id", required = false) String eventId) {

        String body = new String(rawBody, StandardCharsets.UTF_8);

        try {
            String outcome = checkoutFacade.webhook(body, signature, eventId);
            return ResponseEntity.ok(outcome);
        } catch (ForbiddenException e) {
            // Answered here rather than left to the global handler so the reply
            // is a bare status line. There is no human on the other end, and a
            // JSON error body would only ever be read by whoever sent the bad
            // signature - no reason to tell them anything.
            log.warn("Rejected an unsigned or badly signed Razorpay webhook (event {})", eventId);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("invalid signature");
        }
    }
}
