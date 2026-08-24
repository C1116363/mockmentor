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
 * One payment to one mentor, covering a specific set of sessions.
 *
 * <h2>What stops a session being paid twice</h2>
 * Not this table - {@code InterviewRequest.payout}. Creating a payout stamps
 * every session it covers with its id, and only sessions with no stamp are ever
 * picked up. That is the whole mechanism, and it is deliberately not a date
 * range: "everything completed in August" sounds equivalent and is not, because
 * a session completed on the 31st and a payout run on the 30th produce either a
 * gap or an overlap depending on which way somebody rounds. A stamp cannot gap
 * and cannot overlap.
 *
 * <h2>Why the rates are copied here</h2>
 * Same reason PlanEnrollment copies pricePaid. Raising a mentor's rate must not
 * retroactively change what they were already paid last month - and when a
 * mentor asks how a figure was reached, the answer has to be the numbers used at
 * the time, not the ones in force today.
 */
@Entity
@Table(name = "mentor_payouts", indexes = {
        @Index(name = "idx_payout_mentor", columnList = "mentor_id"),
        @Index(name = "idx_payout_status", columnList = "status")
})
public class MentorPayout {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "mentor_id", nullable = false)
    private User mentor;

    /** Completed mock interviews in this payout. */
    @Column(name = "interview_count", nullable = false)
    private int interviewCount;

    /** Completed mentoring sessions in this payout. */
    @Column(name = "mentoring_count", nullable = false)
    private int mentoringCount;

    /** What one interview paid, frozen at the moment this was raised. */
    @Column(name = "interview_rate", nullable = false, precision = 10, scale = 2)
    private BigDecimal interviewRate;

    @Column(name = "mentoring_rate", nullable = false, precision = 10, scale = 2)
    private BigDecimal mentoringRate;

    /**
     * The total.
     *
     * Stored rather than computed on read. It is derivable from the four
     * columns above today, but a later change - a bonus, a deduction, a
     * correction - would silently rewrite the history of every past payout if
     * this were a formula. What was actually paid is a fact, not a calculation.
     */
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    /**
     * The window the covered sessions actually span.
     *
     * Descriptive, not selective - it is derived from the sessions after they
     * are claimed, so it can be shown on a payslip. Nothing is ever chosen by
     * these dates; see the class note.
     */
    @Column(name = "period_start")
    private LocalDateTime periodStart;

    @Column(name = "period_end")
    private LocalDateTime periodEnd;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MentorPayoutStatus status = MentorPayoutStatus.PENDING;

    /** UTR, NEFT reference, whatever the bank gave back. */
    @Column(name = "payment_reference", length = 100)
    private String paymentReference;

    @Column(length = 500)
    private String notes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "paid_by")
    private User paidBy;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @Column(name = "cancelled_reason", length = 500)
    private String cancelledReason;

    protected MentorPayout() {
        // JPA
    }

    public MentorPayout(User mentor, BigDecimal interviewRate, BigDecimal mentoringRate, User createdBy) {
        this.mentor = mentor;
        this.interviewRate = interviewRate;
        this.mentoringRate = mentoringRate;
        this.createdBy = createdBy;
        this.status = MentorPayoutStatus.PENDING;
        this.interviewCount = 0;
        this.mentoringCount = 0;
        this.amount = BigDecimal.ZERO;
    }

    @PrePersist
    void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    /**
     * Fill in the totals once the sessions have actually been claimed.
     *
     * Separate from the constructor because the claim is what decides the
     * counts: the row has to exist before its id can be stamped onto anything,
     * and a concurrent payout run may have taken some of the sessions this one
     * expected. The numbers come from what was won, never from what was
     * intended.
     */
    public void summarise(int interviewCount, int mentoringCount,
                          LocalDateTime periodStart, LocalDateTime periodEnd) {
        this.interviewCount = interviewCount;
        this.mentoringCount = mentoringCount;
        this.periodStart = periodStart;
        this.periodEnd = periodEnd;
        this.amount = interviewRate.multiply(BigDecimal.valueOf(interviewCount))
                .add(mentoringRate.multiply(BigDecimal.valueOf(mentoringCount)));
    }

    public void markPaid(User admin, String reference, String notes) {
        this.status = MentorPayoutStatus.PAID;
        this.paidAt = LocalDateTime.now();
        this.paidBy = admin;
        this.paymentReference = reference;
        this.notes = notes;
    }

    /**
     * Raised in error.
     *
     * Only the status moves here. Releasing the sessions is the caller's job,
     * because it is a bulk update against another table - and doing half of it
     * in the entity would make it far too easy to cancel a payout and quietly
     * strand the work it covered as unpayable.
     */
    public void cancel(String reason) {
        this.status = MentorPayoutStatus.CANCELLED;
        this.cancelledAt = LocalDateTime.now();
        this.cancelledReason = reason;
    }

    public int totalSessions() {
        return interviewCount + mentoringCount;
    }

    public boolean isPending() {
        return status == MentorPayoutStatus.PENDING;
    }

    public Long getId() { return id; }
    public User getMentor() { return mentor; }
    public int getInterviewCount() { return interviewCount; }
    public int getMentoringCount() { return mentoringCount; }
    public BigDecimal getInterviewRate() { return interviewRate; }
    public BigDecimal getMentoringRate() { return mentoringRate; }
    public BigDecimal getAmount() { return amount; }
    public LocalDateTime getPeriodStart() { return periodStart; }
    public LocalDateTime getPeriodEnd() { return periodEnd; }
    public MentorPayoutStatus getStatus() { return status; }
    public String getPaymentReference() { return paymentReference; }
    public String getNotes() { return notes; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public User getCreatedBy() { return createdBy; }
    public LocalDateTime getPaidAt() { return paidAt; }
    public User getPaidBy() { return paidBy; }
    public LocalDateTime getCancelledAt() { return cancelledAt; }
    public String getCancelledReason() { return cancelledReason; }
}
