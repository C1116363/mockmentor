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
