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
 * One mock-interview request.
 *
 * Before we added login, this stored studentName / studentEmail as free text.
 * Now the student is a real account, so those fields are gone - we read the name
 * off the logged-in user instead. That is the whole point of authentication:
 * the server decides who you are, not the browser.
 */
@Entity
@Table(name = "interview_requests")
public class InterviewRequest {

    /** Every interview is one hour. Change this in one place if that changes. */
    public static final int SLOT_MINUTES = 60;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Who raised it. Always a user with role STUDENT. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    /** What they want to be interviewed on, e.g. "Spring Boot backend". */
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

    @Column(length = 2000)
    private String feedback;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected InterviewRequest() {
        // JPA
    }

    public InterviewRequest(User student, String topic, String experienceLevel,
                            LocalDateTime preferredSlot, String notes) {
        this.student = student;
        this.topic = topic;
        this.experienceLevel = experienceLevel;
        this.preferredSlot = preferredSlot;
        this.notes = notes;
        this.status = RequestStatus.PENDING;
    }

    @PrePersist
    void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    /** A mentor picks up this request. */
    public void assignTo(User mentor, LocalDateTime scheduledAt, String meetingLink) {
        this.mentor = mentor;
        this.scheduledAt = scheduledAt;
        this.meetingLink = meetingLink;
        this.status = RequestStatus.SCHEDULED;
    }

    public void complete(String feedback) {
        this.feedback = feedback;
        this.status = RequestStatus.COMPLETED;
    }

    public void cancel() {
        this.status = RequestStatus.CANCELLED;
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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
