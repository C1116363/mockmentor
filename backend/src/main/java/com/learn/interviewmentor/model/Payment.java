package com.learn.interviewmentor.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * One manual UPI payment for one interview.
 *
 * There is no payment gateway in this version: the student pays to our UPI ID
 * from their own app and uploads a screenshot, and an admin confirms the money
 * actually arrived.
 *
 * The amount is stored here rather than read from config at display time, so a
 * later price change never rewrites what somebody already paid.
 */
@Entity
@Table(name = "payments")
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "request_id", nullable = false, unique = true)
    private InterviewRequest request;

    /** What we asked for, fixed at booking time. */
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    /** The UPI transaction / UTR number the student typed in. */
    @Column(name = "upi_reference", length = 40)
    private String upiReference;

    /** Filename on disk. We generate it - never the name the browser sent. */
    @Column(name = "screenshot_file")
    private String screenshotFile;

    @Column(name = "screenshot_content_type", length = 60)
    private String screenshotContentType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentStatus status = PaymentStatus.AWAITING;

    @Column(name = "rejection_reason", length = 500)
    private String rejectionReason;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by")
    private User reviewedBy;

    protected Payment() {
        // JPA
    }

    public Payment(InterviewRequest request, BigDecimal amount) {
        this.request = request;
        this.amount = amount;
        this.status = PaymentStatus.AWAITING;
    }

    /** Student sends proof - also used when resubmitting after a rejection. */
    public void submitProof(String upiReference, String screenshotFile, String contentType) {
        this.upiReference = upiReference;
        this.screenshotFile = screenshotFile;
        this.screenshotContentType = contentType;
        this.status = PaymentStatus.SUBMITTED;
        this.rejectionReason = null;
        this.submittedAt = LocalDateTime.now();
        this.reviewedAt = null;
        this.reviewedBy = null;
    }

    /**
     * The gateway confirmed the money. Nobody reviews it.
     *
     * A gateway payment skips SUBMITTED, because that state means "a human still has
     * to check this". Here the bank has already checked it - re-queueing it for
     * an admin would ask somebody to re-verify a cryptographically signed
     * confirmation, which is both busywork and a delay a paying student can see.
     *
     * upiReference holds the gateway's payment id ("pay_QK3n..."). Reusing the column is
     * deliberate rather than lazy: it means "the reference that proves this was
     * paid", the two id formats cannot be confused for each other, and every
     * screen that already shows it keeps working. Which gateway, which order,
     * and whether it settled by webhook or callback all live on the matching
     * PaymentIntent row.
     *
     * reviewedBy stays null. Nobody reviewed it, and writing in an admin who did not
     * look at it would put a name against a decision they never made.
     */
    public void settleByGateway(String gatewayPaymentId) {
        this.upiReference = gatewayPaymentId;
        this.status = PaymentStatus.VERIFIED;
        this.rejectionReason = null;
        this.submittedAt = LocalDateTime.now();
        this.reviewedAt = this.submittedAt;
        this.reviewedBy = null;
    }

    public void verify(User admin) {
        this.status = PaymentStatus.VERIFIED;
        this.rejectionReason = null;
        this.reviewedAt = LocalDateTime.now();
        this.reviewedBy = admin;
    }

    public void reject(User admin, String reason) {
        this.status = PaymentStatus.REJECTED;
        this.rejectionReason = reason;
        this.reviewedAt = LocalDateTime.now();
        this.reviewedBy = admin;
    }

    public boolean isVerified() {
        return status == PaymentStatus.VERIFIED;
    }

    public Long getId() { return id; }
    public InterviewRequest getRequest() { return request; }
    public BigDecimal getAmount() { return amount; }
    public String getUpiReference() { return upiReference; }
    public String getScreenshotFile() { return screenshotFile; }
    public String getScreenshotContentType() { return screenshotContentType; }
    public PaymentStatus getStatus() { return status; }
    public String getRejectionReason() { return rejectionReason; }
    public LocalDateTime getSubmittedAt() { return submittedAt; }
    public LocalDateTime getReviewedAt() { return reviewedAt; }
    public User getReviewedBy() { return reviewedBy; }
}
