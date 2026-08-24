package com.learn.interviewmentor.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.LocalDateTime;

/**
 * Every webhook the gateway sent us, exactly once.
 *
 * <h2>Two jobs, and the second one is why it earns a table</h2>
 *
 * <b>Idempotency.</b> Razorpay retries until it gets a 2xx, and fires several
 * events for one payment. The unique constraint on {@code event_id} is what
 * makes "money arrived" happen once: a duplicate hits a constraint violation
 * rather than a second activation. Enforced by the database, not by a
 * read-then-write in Java - two retries arriving at the same moment both see no
 * row and both proceed, and that race is the one that grants access twice.
 *
 * <b>The log you will actually want.</b> When a student insists they paid and
 * the app disagrees, the only question that matters is whether the webhook ever
 * arrived. Without this table the answer is a shrug. With it, it is a query.
 * That is also why the raw body is kept: a webhook we failed to parse is
 * precisely the one worth reading by hand, and it is unrecoverable otherwise.
 */
@Entity
@Table(name = "webhook_events",
        uniqueConstraints = @UniqueConstraint(name = "uk_webhook_event_id", columnNames = "event_id"))
public class WebhookEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The gateway's own id for this delivery (Razorpay's X-Razorpay-Event-Id).
     *
     * Unique. This is the whole idempotency mechanism.
     */
    @Column(name = "event_id", nullable = false, length = 120)
    private String eventId;

    /** "order.paid", "payment.failed", and so on. */
    @Column(name = "event_type", nullable = false, length = 60)
    private String eventType;

    /** The order this concerned, where we could find one. For searching by eye. */
    @Column(name = "gateway_order_id", length = 80)
    private String gatewayOrderId;

    /**
     * The exact bytes, as received.
     *
     * Not the parsed object re-serialised. If a signature is ever disputed, the
     * only thing that can settle it is the string the HMAC was computed over.
     */
    @Lob
    @Column(name = "raw_body", nullable = false)
    private String rawBody;

    /** What we did with it, or why we did nothing. Read during an incident. */
    @Column(name = "outcome", length = 300)
    private String outcome;

    @Column(name = "received_at", nullable = false, updatable = false)
    private LocalDateTime receivedAt;

    protected WebhookEvent() {
        // JPA
    }

    public WebhookEvent(String eventId, String eventType, String gatewayOrderId, String rawBody) {
        this.eventId = eventId;
        this.eventType = eventType;
        this.gatewayOrderId = gatewayOrderId;
        this.rawBody = rawBody;
    }

    @PrePersist
    void onCreate() {
        this.receivedAt = LocalDateTime.now();
    }

    public void recordOutcome(String outcome) {
        // Truncated rather than allowed to blow up the insert. Losing the tail
        // of an explanation is a nuisance; losing the row is losing the audit.
        this.outcome = outcome != null && outcome.length() > 300
                ? outcome.substring(0, 300)
                : outcome;
    }

    public Long getId() { return id; }
    public String getEventId() { return eventId; }
    public String getEventType() { return eventType; }
    public String getGatewayOrderId() { return gatewayOrderId; }
    public String getRawBody() { return rawBody; }
    public String getOutcome() { return outcome; }
    public LocalDateTime getReceivedAt() { return receivedAt; }
}
