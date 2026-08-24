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

import java.time.LocalDateTime;

/**
 * A note, PDF or link an admin sends out to students.
 *
 * One row is either a FILE or a LINK, and the columns for the other kind stay
 * null. Two tables would be tidier on paper, but every screen wants them
 * interleaved in one date-ordered list, and a UNION for that is worse than a
 * couple of null columns.
 *
 * Audience is a column and not a join table because a row has exactly one
 * audience - "everyone", "this one student", or "whoever holds this plan". If
 * this ever grows into "these five students", that is the point at which it
 * earns a join table.
 */
@Entity
@Table(name = "study_materials")
public class StudyMaterial {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private MaterialKind kind;

    // ---- FILE only ----

    /** Generated name on disk. Never what the browser sent. */
    @Column(name = "stored_file")
    private String storedFile;

    /** What the student sees as the download name. */
    @Column(name = "original_name", length = 260)
    private String originalName;

    @Column(name = "content_type", length = 120)
    private String contentType;

    @Column(name = "size_bytes")
    private Long sizeBytes;

    // ---- LINK only ----

    @Column(name = "link_url", length = 2000)
    private String linkUrl;

    // ---- audience ----

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MaterialAudience audience = MaterialAudience.ALL_STUDENTS;

    /** Set only when audience is SPECIFIC_STUDENT. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_student_id")
    private User targetStudent;

    /** Set only when audience is PLAN_MEMBERS. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_plan_id")
    private Plan targetPlan;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "uploaded_by", nullable = false)
    private User uploadedBy;

    /** Unpublishing hides it from students without destroying the file. */
    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected StudyMaterial() {
        // JPA
    }

    /** An uploaded file. */
    public static StudyMaterial file(String title, String description, String storedFile,
                                     String originalName, String contentType, long sizeBytes,
                                     User uploadedBy) {
        StudyMaterial m = new StudyMaterial();
        m.title = title;
        m.description = description;
        m.kind = MaterialKind.FILE;
        m.storedFile = storedFile;
        m.originalName = originalName;
        m.contentType = contentType;
        m.sizeBytes = sizeBytes;
        m.uploadedBy = uploadedBy;
        return m;
    }

    /** A link to something hosted elsewhere. */
    public static StudyMaterial link(String title, String description, String linkUrl,
                                     User uploadedBy) {
        StudyMaterial m = new StudyMaterial();
        m.title = title;
        m.description = description;
        m.kind = MaterialKind.LINK;
        m.linkUrl = linkUrl;
        m.uploadedBy = uploadedBy;
        return m;
    }

    @PrePersist
    void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public void sendToEveryone() {
        this.audience = MaterialAudience.ALL_STUDENTS;
        this.targetStudent = null;
        this.targetPlan = null;
    }

    public void sendTo(User student) {
        this.audience = MaterialAudience.SPECIFIC_STUDENT;
        this.targetStudent = student;
        this.targetPlan = null;
    }

    public void sendToPlan(Plan plan) {
        this.audience = MaterialAudience.PLAN_MEMBERS;
        this.targetPlan = plan;
        this.targetStudent = null;
    }

    public Long getId() { return id; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public MaterialKind getKind() { return kind; }
    public String getStoredFile() { return storedFile; }
    public String getOriginalName() { return originalName; }
    public String getContentType() { return contentType; }
    public Long getSizeBytes() { return sizeBytes; }
    public String getLinkUrl() { return linkUrl; }
    public MaterialAudience getAudience() { return audience; }
    public User getTargetStudent() { return targetStudent; }
    public Plan getTargetPlan() { return targetPlan; }
    public User getUploadedBy() { return uploadedBy; }
    public boolean isActive() { return active; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    public void setTitle(String title) { this.title = title; }
    public void setDescription(String description) { this.description = description; }
    public void setActive(boolean active) { this.active = active; }
}
