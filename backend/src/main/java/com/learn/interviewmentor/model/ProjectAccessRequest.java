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
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * One student asking for - and paying for - contributor access to one project.
 *
 * Same manual-UPI shape as a plan purchase, with one thing that makes it
 * genuinely different: granting it has an effect **outside this database**. A
 * plan going ACTIVE only changes what our own screens show. This going ACTIVE
 * means a real GitHub account gains push access to a real private repository.
 *
 * Two columns exist because of that:
 *
 * <ul>
 *   <li>{@link #githubUsername} - we cannot grant access without it, and it is
 *       the student's to supply. It is captured at request time rather than at
 *       grant time so an admin is never sitting on a verified payment waiting
 *       for somebody to answer an email.</li>
 *   <li>{@link #collaboratorGranted} - whether the GitHub side actually happened,
 *       kept separate from {@code status}. The payment being verified and the
 *       collaborator invite succeeding are two events, and conflating them means
 *       a failed invite looks like granted access.</li>
 * </ul>
 */
@Entity
@Table(name = "project_access_requests")
public class ProjectAccessRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false)
    private LiveProject project;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    /**
     * The GitHub account to add as a collaborator.
     *
     * Not derived from the email: plenty of people's GitHub handle has nothing to
     * do with the address they signed up with, and inviting the wrong account
     * gives a stranger push access to a private repo.
     */
    @Column(name = "github_username", nullable = false, length = 39)
    private String githubUsername;

    /** Why they want in. Gives the reviewer something to judge besides a payment. */
    @Lob
    @Column(columnDefinition = "TEXT")
    private String motivation;

    /** Frozen at request time, so a later price change never rewrites it. */
    @Column(name = "price_paid", nullable = false, precision = 10, scale = 2)
    private BigDecimal pricePaid;

    /** Copied too, so shortening a project's window can't shorten existing access. */
    @Column(name = "access_duration_days", nullable = false)
    private int accessDurationDays;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ProjectAccessStatus status = ProjectAccessStatus.AWAITING_PAYMENT;

    /**
     * Did the GitHub invite actually go through?
     *
     * Deliberately not folded into {@code status}. When this is false on an
     * ACTIVE row, somebody paid and is waiting - which is exactly the queue an
     * admin needs to see, and it would be invisible if one column meant both.
     */
    @Column(name = "collaborator_granted", nullable = false)
    private boolean collaboratorGranted = false;

    /** What went wrong on the GitHub side, if it did. Admin-facing. */
    @Column(name = "grant_error", length = 500)
    private String grantError;

    // ---- payment ----

    @Column(name = "upi_reference", length = 40)
    private String upiReference;

    @Column(name = "screenshot_file")
    private String screenshotFile;

    @Column(name = "screenshot_content_type", length = 60)
    private String screenshotContentType;

    @Column(name = "rejection_reason", length = 500)
    private String rejectionReason;

    // ---- timeline ----

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by")
    private User reviewedBy;

    @Column(name = "granted_at")
    private LocalDateTime grantedAt;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;

    @Column(name = "revoked_reason", length = 500)
    private String revokedReason;

    protected ProjectAccessRequest() {
        // JPA
    }

    public ProjectAccessRequest(LiveProject project, User student,
                                String githubUsername, String motivation) {
        this.project = project;
        this.student = student;
        this.githubUsername = githubUsername;
        this.motivation = motivation;
        this.pricePaid = project.getPrice();
        this.accessDurationDays = project.getAccessDurationDays();
        this.status = ProjectAccessStatus.AWAITING_PAYMENT;
    }

    @PrePersist
    void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    /** Student sends proof. Also the resubmit path after a rejection. */
    public void submitProof(String upiReference, String screenshotFile, String contentType) {
        this.upiReference = upiReference;
        this.screenshotFile = screenshotFile;
        this.screenshotContentType = contentType;
        this.status = ProjectAccessStatus.SUBMITTED;
        this.rejectionReason = null;
        this.submittedAt = LocalDateTime.now();
        this.reviewedAt = null;
        this.reviewedBy = null;
    }

    /**
     * Admin confirmed the money. This starts the access window.
     *
     * Note it does NOT set collaboratorGranted - that is the GitHub side, and it
     * is recorded separately by {@link #markCollaboratorGranted()} once the
     * invite has actually succeeded.
     */
    public void approve(User admin) {
        this.status = ProjectAccessStatus.ACTIVE;
        this.rejectionReason = null;
        this.reviewedAt = LocalDateTime.now();
        this.reviewedBy = admin;
        this.grantedAt = this.reviewedAt;
        this.expiresAt = this.grantedAt.plusDays(accessDurationDays);
    }

    public void markCollaboratorGranted() {
        this.collaboratorGranted = true;
        this.grantError = null;
    }

    public void markGrantFailed(String reason) {
        this.collaboratorGranted = false;
        this.grantError = reason;
    }

    public void reject(User admin, String reason) {
        this.status = ProjectAccessStatus.REJECTED;
        this.rejectionReason = reason;
        this.reviewedAt = LocalDateTime.now();
        this.reviewedBy = admin;
    }

    public void cancel() {
        this.status = ProjectAccessStatus.CANCELLED;
    }

    /** An admin taking access away early - misuse, or the project wrapping up. */
    public void revoke(String reason) {
        this.status = ProjectAccessStatus.REVOKED;
        this.revokedAt = LocalDateTime.now();
        this.revokedReason = reason;
        this.collaboratorGranted = false;
    }

    /**
     * ACTIVE and still inside its window.
     *
     * The date is checked here rather than trusted from the status column -
     * nothing sweeps this table at midnight, so an ACTIVE row can be past its
     * expiry. This is the field every access decision should read.
     */
    public boolean isCurrentlyActive() {
        return status == ProjectAccessStatus.ACTIVE
                && (expiresAt == null || expiresAt.isAfter(LocalDateTime.now()));
    }

    /**
     * Paid and approved, but the GitHub invite has not gone through.
     *
     * This is the queue that matters operationally: somebody has paid and cannot
     * see the code yet.
     */
    public boolean isAwaitingCollaboratorInvite() {
        return isCurrentlyActive() && !collaboratorGranted;
    }

    /** Access that has run out but is still marked ACTIVE - the sweep queue. */
    public boolean isPastExpiry() {
        return status == ProjectAccessStatus.ACTIVE
                && expiresAt != null && !expiresAt.isAfter(LocalDateTime.now());
    }

    public boolean isOwnedBy(User user) {
        return student != null && user != null && student.getId().equals(user.getId());
    }

    public Long getId() { return id; }
    public LiveProject getProject() { return project; }
    public User getStudent() { return student; }
    public String getGithubUsername() { return githubUsername; }
    public String getMotivation() { return motivation; }
    public BigDecimal getPricePaid() { return pricePaid; }
    public int getAccessDurationDays() { return accessDurationDays; }
    public ProjectAccessStatus getStatus() { return status; }
    public boolean isCollaboratorGranted() { return collaboratorGranted; }
    public String getGrantError() { return grantError; }
    public String getUpiReference() { return upiReference; }
    public String getScreenshotFile() { return screenshotFile; }
    public String getScreenshotContentType() { return screenshotContentType; }
    public String getRejectionReason() { return rejectionReason; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getSubmittedAt() { return submittedAt; }
    public LocalDateTime getReviewedAt() { return reviewedAt; }
    public User getReviewedBy() { return reviewedBy; }
    public LocalDateTime getGrantedAt() { return grantedAt; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public LocalDateTime getRevokedAt() { return revokedAt; }
    public String getRevokedReason() { return revokedReason; }

    /** The student can correct a typo before it is granted. */
    public void setGithubUsername(String githubUsername) {
        this.githubUsername = githubUsername;
    }
}
