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
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * One booked hour with an expert - either a mock interview or a mentoring
 * discussion. {@link SessionType} says which.
 *
 * Before we added login, this stored studentName / studentEmail as free text.
 * Now the student is a real account, so those fields are gone - we read the name
 * off the logged-in user instead. That is the whole point of authentication:
 * the server decides who you are, not the browser.
 */
@Entity
@Table(name = "interview_requests")
public class InterviewRequest {

    /** Every session is one hour. Change this in one place if that changes. */
    public static final int SLOT_MINUTES = 60;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Who raised it. Always a user with role STUDENT. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    /**
     * Interview or discussion.
     *
     * Deliberately nullable in the database even though the code always sets it.
     * The column was added to a table that already had rows, and with
     * ddl-auto=update there is no migration step to backfill them: MySQL fills a
     * NOT NULL column with an implicit default, which for an enum stored as a
     * string is the empty string - and that then fails to map back to any
     * constant, so every old booking would throw on read. Nullable plus the
     * coalescing getter below means old rows simply read as MOCK_INTERVIEW,
     * which is what they were.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "session_type", length = 20)
    private SessionType sessionType = SessionType.MOCK_INTERVIEW;

    /** What they want to cover, e.g. "Spring Boot backend" or "career advice". */
    @Column(nullable = false)
    private String topic;

    /** Experience level of the student: e.g. "Fresher", "1-3 years". */
    @Column(name = "experience_level", nullable = false)
    private String experienceLevel;

    /**
     * Start of the 1-hour slot the candidate picked, e.g. 2026-09-20T15:00.
     * Slots always start on the hour and always run for SLOT_MINUTES.
     */
    @Column(name = "preferred_slot", nullable = false)
    private LocalDateTime preferredSlot;

    @Column(length = 1000)
    private String notes;

    // ---------- the candidate's CV (optional) ----------

    /**
     * Attached per booking, not held once on the account.
     *
     * A CV changes - somebody books in March and again in August with a job in
     * between - and an interviewer needs the version that was current for the
     * session they are running, not whatever was uploaded last. Storing it on the
     * booking also means deleting a booking takes its CV with it.
     */
    @Column(name = "cv_file")
    private String cvFile;

    /** What the candidate called it. Used as the download name. */
    @Column(name = "cv_original_name", length = 200)
    private String cvOriginalName;

    @Column(name = "cv_content_type", length = 120)
    private String cvContentType;

    @Column(name = "cv_size_bytes")
    private Long cvSizeBytes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RequestStatus status = RequestStatus.PENDING;

    /** Null until a mentor accepts. Always a user with role MENTOR. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mentor_id")
    private User mentor;

    @Column(name = "scheduled_at")
    private LocalDateTime scheduledAt;

    @Column(name = "meeting_link")
    private String meetingLink;

    // ---------- feedback (all null until the mentor completes it) ----------

    /** Free-text summary the mentor writes. */
    @Column(length = 2000)
    private String feedback;

    /** What went well. */
    @Column(length = 2000)
    private String strengths;

    /** What to work on before the real interview. */
    @Column(length = 2000)
    private String improvements;

    /**
     * Scores out of 5. Integer rather than int so "not rated" is distinct from
     * a score of zero - older interviews completed before scoring existed have
     * these as null.
     */
    @Column(name = "overall_rating")
    private Integer overallRating;

    @Column(name = "technical_rating")
    private Integer technicalRating;

    @Column(name = "communication_rating")
    private Integer communicationRating;

    @Column(name = "problem_solving_rating")
    private Integer problemSolvingRating;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private Recommendation recommendation;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected InterviewRequest() {
        // JPA
    }

    public InterviewRequest(User student, SessionType sessionType, String topic,
                            String experienceLevel, LocalDateTime preferredSlot, String notes) {
        this.student = student;
        this.sessionType = sessionType == null ? SessionType.MOCK_INTERVIEW : sessionType;
        this.topic = topic;
        this.experienceLevel = experienceLevel;
        this.preferredSlot = preferredSlot;
        this.notes = notes;
        // Nothing is visible to mentors until the money is confirmed.
        this.status = RequestStatus.AWAITING_PAYMENT;
    }

    @PrePersist
    void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    /**
     * Payment confirmed - the request becomes visible to mentors.
     * Called only from PaymentService when an admin verifies the transfer.
     */
    public void markPaid() {
        this.status = RequestStatus.PENDING;
    }

    /** A mentor picks up this request. */
    public void assignTo(User mentor, LocalDateTime scheduledAt, String meetingLink) {
        this.mentor = mentor;
        this.scheduledAt = scheduledAt;
        this.meetingLink = meetingLink;
        this.status = RequestStatus.SCHEDULED;
    }

    /**
     * The mentor closes the session.
     *
     * For a mock interview this is the scorecard: everything except the summary
     * is optional. For a mentoring session the ratings are dropped on the floor
     * rather than stored - a client that sends them anyway must not be able to
     * staple a fake scorecard onto a discussion, and this is the last place that
     * can be guaranteed regardless of which caller got here.
     */
    public void complete(String feedback, String strengths, String improvements,
                         Integer overallRating, Integer technicalRating,
                         Integer communicationRating, Integer problemSolvingRating,
                         Recommendation recommendation) {
        this.feedback = feedback;
        this.strengths = strengths;
        this.improvements = improvements;

        boolean scored = getSessionType().isScored();
        this.overallRating = scored ? overallRating : null;
        this.technicalRating = scored ? technicalRating : null;
        this.communicationRating = scored ? communicationRating : null;
        this.problemSolvingRating = scored ? problemSolvingRating : null;
        this.recommendation = scored ? recommendation : null;

        this.status = RequestStatus.COMPLETED;
        this.completedAt = LocalDateTime.now();
    }

    public void cancel() {
        this.status = RequestStatus.CANCELLED;
    }

    /**
     * When the mentor wrote this up. Null for anything not finished.
     *
     * <h2>Null on old rows, and that is a migration trap</h2>
     * ddl-auto=update adds this column to a table that already has completed
     * interviews in it, and every one of them gets null - the information was
     * simply never recorded. Payroll reads it to work out the period a payout
     * covers, so {@link #getCompletedAt()} falls back rather than returning
     * null and putting a hole in a payslip.
     */
    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    /**
     * The payout that paid for this session, or null if it has not been paid.
     *
     * <h2>This field is the whole anti-double-pay mechanism</h2>
     * Payroll picks up completed sessions where this is null, and stamps them
     * as it goes. A session can therefore appear in exactly one payout, whatever
     * order the runs happen in and however many admins are clicking at once -
     * the claim is a single UPDATE with {@code payout_id IS NULL} in its WHERE,
     * so the database decides the winner rather than the application.
     *
     * The alternative - paying "everything completed between two dates" - looks
     * equivalent and is not: a session completed while the run is in progress
     * lands on one side or the other depending on rounding, and nobody notices
     * until a mentor is paid twice or not at all.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payout_id")
    private MentorPayout payout;

    public MentorPayout getPayout() {
        return payout;
    }

    public boolean isPaidOut() {
        return payout != null;
    }

    /**
     * Attach or replace the CV.
     *
     * @return the filename this replaced, so the caller can delete it. A booking
     *         keeps one CV, not a pile of old ones - and returning it rather than
     *         deleting here keeps the entity free of file-system concerns.
     */
    public String attachCv(String file, String originalName, String contentType, long sizeBytes) {
        String previous = this.cvFile;
        this.cvFile = file;
        this.cvOriginalName = originalName;
        this.cvContentType = contentType;
        this.cvSizeBytes = sizeBytes;
        return previous;
    }

    public boolean hasCv() {
        return cvFile != null;
    }

    /** Used by the service to check "is this actually your request?". */
    public boolean isOwnedBy(User user) {
        return student != null && student.getId().equals(user.getId());
    }

    public boolean isMentoredBy(User user) {
        return mentor != null && mentor.getId().equals(user.getId());
    }

    public Long getId() {
        return id;
    }

    public User getStudent() {
        return student;
    }

    /**
     * Never null, even for rows written before this column existed - see the
     * field comment. Every read goes through here so no caller has to remember.
     */
    public SessionType getSessionType() {
        return sessionType == null ? SessionType.MOCK_INTERVIEW : sessionType;
    }

    public String getTopic() {
        return topic;
    }

    public String getExperienceLevel() {
        return experienceLevel;
    }

    public LocalDateTime getPreferredSlot() {
        return preferredSlot;
    }

    /** When the slot finishes - derived, not stored. */
    public LocalDateTime getPreferredSlotEnd() {
        return preferredSlot == null ? null : preferredSlot.plusMinutes(SLOT_MINUTES);
    }

    public String getNotes() {
        return notes;
    }

    public RequestStatus getStatus() {
        return status;
    }

    public User getMentor() {
        return mentor;
    }

    public LocalDateTime getScheduledAt() {
        return scheduledAt;
    }

    public String getMeetingLink() {
        return meetingLink;
    }

    public String getFeedback() {
        return feedback;
    }

    public String getStrengths() {
        return strengths;
    }

    public String getImprovements() {
        return improvements;
    }

    public Integer getOverallRating() {
        return overallRating;
    }

    public Integer getTechnicalRating() {
        return technicalRating;
    }

    public Integer getCommunicationRating() {
        return communicationRating;
    }

    public Integer getProblemSolvingRating() {
        return problemSolvingRating;
    }

    public Recommendation getRecommendation() {
        return recommendation;
    }

    /**
     * When it was completed, falling back for rows that predate the column.
     *
     * scheduledAt is the closest honest answer - it is when the session
     * actually happened - and createdAt is the last resort. Better a payslip
     * that says roughly when than one with a blank where a date should be.
     */
    public LocalDateTime getCompletedAt() {
        if (completedAt != null) {
            return completedAt;
        }
        return scheduledAt != null ? scheduledAt : createdAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public String getCvFile() {
        return cvFile;
    }

    public String getCvOriginalName() {
        return cvOriginalName;
    }

    public String getCvContentType() {
        return cvContentType;
    }

    public Long getCvSizeBytes() {
        return cvSizeBytes;
    }
}
