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
import jakarta.persistence.UniqueConstraint;

import java.time.LocalDateTime;

/**
 * One hour a mentor has said they are free.
 *
 * This is what changed the booking model. Before, students were shown a generated
 * 9-to-9 grid and capacity was "how many verified mentors exist" - which meant the
 * app happily sold a 7 AM Sunday slot that nobody had agreed to take. Now the grid
 * is the union of what mentors actually declared, so an offered slot is one a real
 * person has committed to.
 *
 * <h2>Two booleans rather than a session-type enum</h2>
 * A mentor may be happy to run a mock interview but not a career discussion, or
 * the reverse - and plenty will do both. Two flags say that directly. An enum
 * would need a BOTH member, and a third session type later would turn three
 * members into seven.
 */
@Entity
@Table(name = "mentor_availability",
       // One row per mentor per hour. Without this a mentor could declare the
       // same slot twice and inflate that hour's capacity on their own.
       uniqueConstraints = @UniqueConstraint(
               name = "uk_mentor_slot", columnNames = {"mentor_id", "slot_start"}))
public class MentorAvailability {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "mentor_id", nullable = false)
    private User mentor;

    /** Start of the one-hour slot. Always on the hour. */
    @Column(name = "slot_start", nullable = false)
    private LocalDateTime slotStart;

    @Column(name = "for_interviews", nullable = false)
    private boolean forInterviews = true;

    @Column(name = "for_mentoring", nullable = false)
    private boolean forMentoring = true;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AvailabilityStatus status = AvailabilityStatus.OPEN;

    /**
     * The booking an admin mapped onto this hour.
     *
     * Kept as a link rather than just a flag so the admin screen can say *who*
     * took the slot, and so releasing it when a booking is cancelled is a lookup
     * rather than a guess.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booked_request_id")
    private InterviewRequest bookedRequest;

    /** Anything the mentor wants the admin to know about this hour. */
    @Column(length = 300)
    private String note;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected MentorAvailability() {
        // JPA
    }

    public MentorAvailability(User mentor, LocalDateTime slotStart,
                              boolean forInterviews, boolean forMentoring, String note) {
        this.mentor = mentor;
        this.slotStart = slotStart;
        this.forInterviews = forInterviews;
        this.forMentoring = forMentoring;
        this.note = note;
        this.status = AvailabilityStatus.OPEN;
    }

    @PrePersist
    void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public LocalDateTime getSlotEnd() {
        return slotStart.plusMinutes(InterviewRequest.SLOT_MINUTES);
    }

    /** Does this hour cover the kind of session being asked about? */
    public boolean covers(SessionType type) {
        return type == SessionType.MENTORING ? forMentoring : forInterviews;
    }

    /** Free to be assigned: declared, not withdrawn, nobody on it. */
    public boolean isOpen() {
        return status == AvailabilityStatus.OPEN && bookedRequest == null;
    }

    public void bookFor(InterviewRequest request) {
        this.bookedRequest = request;
        this.status = AvailabilityStatus.BOOKED;
    }

    /**
     * Put the hour back on the market.
     *
     * Called when a booking is cancelled - the mentor is free again and there is
     * no reason that hour should stay dark.
     */
    public void release() {
        this.bookedRequest = null;
        this.status = AvailabilityStatus.OPEN;
    }

    public void withdraw() {
        this.status = AvailabilityStatus.WITHDRAWN;
    }

    /** A mentor can change what kinds of session they will take, while it is open. */
    public void updateOffering(boolean forInterviews, boolean forMentoring, String note) {
        this.forInterviews = forInterviews;
        this.forMentoring = forMentoring;
        this.note = note;
    }

    public boolean isOwnedBy(User user) {
        return mentor != null && user != null && mentor.getId().equals(user.getId());
    }

    public Long getId() { return id; }
    public User getMentor() { return mentor; }
    public LocalDateTime getSlotStart() { return slotStart; }
    public boolean isForInterviews() { return forInterviews; }
    public boolean isForMentoring() { return forMentoring; }
    public AvailabilityStatus getStatus() { return status; }
    public InterviewRequest getBookedRequest() { return bookedRequest; }
    public String getNote() { return note; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
