package com.learn.interviewmentor.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * One attempt to pay for one thing through a gateway.
 *
 * <h2>Why this table exists at all</h2>
 * A webhook arrives carrying a gateway order id and nothing else we recognise.
 * Something has to already know that order {@code order_XYZ} is the ₹499 for
 * enrollment 14 belonging to student 7. That is this row, written <i>before</i>
 * the student is sent to the checkout - because after the money moves is far too
 * late to start guessing what it was for.
 *
 * <h2>Why an attempt, not a payment</h2>
 * One enrollment can have several of these: a card declined, a UPI request that
 * timed out, then one that worked. Overwriting a single row each time would
 * destroy exactly the history you want when a student says they were charged
 * twice. So intents accumulate and at most one reaches PAID.
 *
 * <h2>The amount is copied here</h2>
 * Frozen at checkout time, for the same reason {@code PlanEnrollment.pricePaid}
 * is frozen - and one more: this is the number we can compare against what the
 * gateway says was actually captured. A mismatch means the amount was tampered
 * with somewhere between here and the bank, and that check is only possible if
 * we wrote down what we asked for.
 */
@Entity
@Table(name = "payment_intents", indexes = {
        // The webhook's lookup. Every settlement does exactly this query, and
        // without the index it is a full scan of a table that only grows.
        @Index(name = "idx_intent_order", columnList = "gateway_order_id", unique = true),
        // "Is there already an open intent for this thing?" - asked on every
        // checkout to avoid opening a second order for a row already being paid.
        @Index(name = "idx_intent_target", columnList = "purpose, target_id")
})
public class PaymentIntent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentPurpose purpose;

    /**
     * The id of the row this pays for - a Payment, PlanEnrollment or
     * ProjectAccessRequest, depending on {@link #purpose}.
     *
     * Deliberately a plain Long rather than three nullable foreign keys. Three
     * columns of which exactly two are always null is a table that lies about
     * its own shape, and every query against it has to know the rule anyway. The
     * cost is no database-level referential integrity here; the settlement code
     * resolves the row through the owning service and fails loudly if it is gone.
     */
    @Column(name = "target_id", nullable = false)
    private Long targetId;

    /**
     * Who is paying.
     *
     * Not derivable from targetId without loading it, and needed on the way in:
     * a student must not be able to open a checkout against somebody else's
     * enrollment, and this is what that check compares against.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    /** What we asked the gateway for. Compared against what it says it took. */
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    /**
     * The gateway's order id.
     *
     * Unique, and the reason the webhook can find this row. Nullable only for
     * the instant between constructing the intent and the gateway answering.
     */
    @Column(name = "gateway_order_id", length = 80)
    private String gatewayOrderId;

    /** The gateway's id for the payment that settled it. Null until PAID. */
    @Column(name = "gateway_payment_id", length = 80)
    private String gatewayPaymentId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentIntentStatus status = PaymentIntentStatus.CREATED;

    /** The gateway's own words when a payment failed. Shown to the student. */
    @Column(name = "failure_reason", length = 500)
    private String failureReason;

    /**
     * Which route settled this - "webhook" or "callback".
     *
     * Worth a column. When a settlement is disputed, the first question is
     * whether we learned about it from the gateway server-to-server or from a
     * browser, and those are not equally trustworthy.
     */
    @Column(name = "settled_via", length = 20)
    private String settledVia;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    protected PaymentIntent() {
        // JPA
    }

    public PaymentIntent(PaymentPurpose purpose, Long targetId, User student, BigDecimal amount) {
        this.purpose = purpose;
        this.targetId = targetId;
        this.student = student;
        this.amount = amount;
        this.status = PaymentIntentStatus.CREATED;
    }

    @PrePersist
    void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public void attachOrder(String gatewayOrderId) {
        this.gatewayOrderId = gatewayOrderId;
    }

    /**
     * Money captured.
     *
     * Returns whether this call is the one that changed anything, so the caller
     * can activate the purchased thing exactly once. Razorpay retries a webhook
     * until it gets a 2xx and also fires more than one event for a single
     * payment, so "settle" is called repeatedly for the same money as a matter
     * of course - not as an edge case. Every duplicate lands here and returns
     * false, and access is granted once.
     */
    public boolean settle(String gatewayPaymentId, String via) {
        if (this.status == PaymentIntentStatus.PAID) {
            return false;
        }
        this.status = PaymentIntentStatus.PAID;
        this.gatewayPaymentId = gatewayPaymentId;
        this.settledVia = via;
        this.paidAt = LocalDateTime.now();
        this.failureReason = null;
        return true;
    }

    /**
     * The gateway says this attempt failed.
     *
     * Never overwrites PAID. Razorpay's events are not ordered, so a
     * payment.failed for an earlier card attempt can arrive after the
     * order.paid that succeeded - and letting it through would revoke access
     * somebody has already paid for.
     */
    public void fail(String reason) {
        if (this.status == PaymentIntentStatus.PAID) {
            return;
        }
        this.status = PaymentIntentStatus.FAILED;
        this.failureReason = reason;
    }

    public void abandon() {
        if (this.status == PaymentIntentStatus.CREATED) {
            this.status = PaymentIntentStatus.ABANDONED;
        }
    }

    public boolean isPaid() {
        return status == PaymentIntentStatus.PAID;
    }

    public boolean isOwnedBy(User user) {
        return student != null && user != null && student.getId().equals(user.getId());
    }

    public Long getId() { return id; }
    public PaymentPurpose getPurpose() { return purpose; }
    public Long getTargetId() { return targetId; }
    public User getStudent() { return student; }
    public BigDecimal getAmount() { return amount; }
    public String getGatewayOrderId() { return gatewayOrderId; }
    public String getGatewayPaymentId() { return gatewayPaymentId; }
    public PaymentIntentStatus getStatus() { return status; }
    public String getFailureReason() { return failureReason; }
    public String getSettledVia() { return settledVia; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getPaidAt() { return paidAt; }
}
