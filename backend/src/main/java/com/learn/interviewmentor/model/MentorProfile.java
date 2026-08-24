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
 * Everything a mentor submits so an admin can verify them.
 *
 * ⚠️ This holds real personal data - Aadhaar and bank details. In a production
 * system those columns would be encrypted at rest, access-logged, and covered by
 * a retention policy. They are stored in plain text here because this is a
 * learning project; see the README before putting anything like this live.
 *
 * Nothing sensitive is ever returned in full: the DTOs mask everything down to
 * the last four digits.
 */
@Entity
@Table(name = "mentor_profiles")
public class MentorProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    // ---------- professional ----------

    /** e.g. "Java, Spring Boot, System Design" */
    @Column
    private String expertise;

    @Column(name = "years_of_experience")
    private int yearsOfExperience;

    @Column(name = "current_company")
    private String currentCompany;

    /** Job title, e.g. "Senior Software Engineer". */
    @Column(name = "current_role_title")
    private String currentRoleTitle;

    @Column(length = 500)
    private String bio;

    @Column(name = "linkedin_url")
    private String linkedinUrl;

    // ---------- education ----------

    @Column(name = "highest_qualification")
    private String highestQualification;

    @Column
    private String university;

    @Column(name = "graduation_year")
    private Integer graduationYear;

    // ---------- contact ----------

    @Column(name = "phone_number")
    private String phoneNumber;

    // ---------- KYC (sensitive) ----------

    @Column(name = "aadhaar_number", length = 12)
    private String aadhaarNumber;

    @Column(name = "pan_number", length = 10)
    private String panNumber;

    // ---------- bank (sensitive, for paying the mentor) ----------

    @Column(name = "bank_account_holder")
    private String bankAccountHolder;

    @Column(name = "bank_account_number", length = 18)
    private String bankAccountNumber;

    @Column(name = "bank_ifsc", length = 11)
    private String bankIfsc;

    @Column(name = "bank_name")
    private String bankName;

    // ---------- verification ----------

    @Enumerated(EnumType.STRING)
    @Column(name = "verification_status", nullable = false, length = 20)
    private VerificationStatus verificationStatus = VerificationStatus.INCOMPLETE;

    @Column(name = "rejection_reason", length = 500)
    private String rejectionReason;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    /** Which admin made the call, so decisions are traceable. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by")
    private User reviewedBy;

    // ---------- payroll ----------

    /**
     * Whether this mentor is on payroll.
     *
     * Off until an admin turns it on, so a newly verified mentor cannot appear
     * in a payout run at a rate nobody chose. It is also the switch for someone
     * who has stopped taking sessions: turning it off stops new payouts without
     * touching what they have already earned.
     */
    @Column(name = "payroll_enabled", nullable = false)
    private boolean payrollEnabled = false;

    /**
     * Paid per completed mock interview.
     *
     * Per session rather than hourly, because a session is the unit the app
     * actually records - there is no clock on it, so an hourly rate would be a
     * number multiplied by an assumption.
     */
    @Column(name = "interview_rate", precision = 10, scale = 2)
    private BigDecimal interviewRate;

    /**
     * Paid per completed mentoring session.
     *
     * Its own rate rather than a share of the interview one: a mock interview
     * ends in a written scorecard and a mentoring session does not, so they are
     * different amounts of work and most places pay them differently.
     */
    @Column(name = "mentoring_rate", precision = 10, scale = 2)
    private BigDecimal mentoringRate;

    /** Enabled, and both rates actually set. Payouts need all three. */
    public boolean isPayrollReady() {
        return payrollEnabled && interviewRate != null && mentoringRate != null;
    }

    public void configurePayroll(boolean enabled, BigDecimal interviewRate, BigDecimal mentoringRate) {
        this.payrollEnabled = enabled;
        this.interviewRate = interviewRate;
        this.mentoringRate = mentoringRate;
    }

    public boolean isPayrollEnabled() { return payrollEnabled; }
    public BigDecimal getInterviewRate() { return interviewRate; }
    public BigDecimal getMentoringRate() { return mentoringRate; }

    protected MentorProfile() {
        // JPA
    }

    /** A blank profile, created the moment a mentor signs up. */
    public MentorProfile(User user) {
        this.user = user;
        this.verificationStatus = VerificationStatus.INCOMPLETE;
    }

    /** Used by the seeder for mentors who are already verified. */
    public MentorProfile(User user, String expertise, int yearsOfExperience,
                         String currentCompany, String bio) {
        this.user = user;
        this.expertise = expertise;
        this.yearsOfExperience = yearsOfExperience;
        this.currentCompany = currentCompany;
        this.bio = bio;
        this.verificationStatus = VerificationStatus.APPROVED;
        this.submittedAt = LocalDateTime.now();
        this.reviewedAt = LocalDateTime.now();
    }

    /** Mentor submits (or resubmits) for review. */
    public void submitForReview() {
        this.verificationStatus = VerificationStatus.PENDING;
        this.rejectionReason = null;
        this.submittedAt = LocalDateTime.now();
        this.reviewedAt = null;
        this.reviewedBy = null;
    }

    public void approve(User admin) {
        this.verificationStatus = VerificationStatus.APPROVED;
        this.rejectionReason = null;
        this.reviewedAt = LocalDateTime.now();
        this.reviewedBy = admin;
    }

    public void reject(User admin, String reason) {
        this.verificationStatus = VerificationStatus.REJECTED;
        this.rejectionReason = reason;
        this.reviewedAt = LocalDateTime.now();
        this.reviewedBy = admin;
    }

    public boolean isApproved() {
        return verificationStatus == VerificationStatus.APPROVED;
    }

    // ---------- getters / setters ----------

    public Long getId() { return id; }
    public User getUser() { return user; }

    public String getExpertise() { return expertise; }
    public void setExpertise(String expertise) { this.expertise = expertise; }

    public int getYearsOfExperience() { return yearsOfExperience; }
    public void setYearsOfExperience(int yearsOfExperience) { this.yearsOfExperience = yearsOfExperience; }

    public String getCurrentCompany() { return currentCompany; }
    public void setCurrentCompany(String currentCompany) { this.currentCompany = currentCompany; }

    public String getCurrentRoleTitle() { return currentRoleTitle; }
    public void setCurrentRoleTitle(String currentRoleTitle) { this.currentRoleTitle = currentRoleTitle; }

    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }

    public String getLinkedinUrl() { return linkedinUrl; }
    public void setLinkedinUrl(String linkedinUrl) { this.linkedinUrl = linkedinUrl; }

    public String getHighestQualification() { return highestQualification; }
    public void setHighestQualification(String highestQualification) { this.highestQualification = highestQualification; }

    public String getUniversity() { return university; }
    public void setUniversity(String university) { this.university = university; }

    public Integer getGraduationYear() { return graduationYear; }
    public void setGraduationYear(Integer graduationYear) { this.graduationYear = graduationYear; }

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    public String getAadhaarNumber() { return aadhaarNumber; }
    public void setAadhaarNumber(String aadhaarNumber) { this.aadhaarNumber = aadhaarNumber; }

    public String getPanNumber() { return panNumber; }
    public void setPanNumber(String panNumber) { this.panNumber = panNumber; }

    public String getBankAccountHolder() { return bankAccountHolder; }
    public void setBankAccountHolder(String bankAccountHolder) { this.bankAccountHolder = bankAccountHolder; }

    public String getBankAccountNumber() { return bankAccountNumber; }
    public void setBankAccountNumber(String bankAccountNumber) { this.bankAccountNumber = bankAccountNumber; }

    public String getBankIfsc() { return bankIfsc; }
    public void setBankIfsc(String bankIfsc) { this.bankIfsc = bankIfsc; }

    public String getBankName() { return bankName; }
    public void setBankName(String bankName) { this.bankName = bankName; }

    public VerificationStatus getVerificationStatus() { return verificationStatus; }
    public String getRejectionReason() { return rejectionReason; }
    public LocalDateTime getSubmittedAt() { return submittedAt; }
    public LocalDateTime getReviewedAt() { return reviewedAt; }
    public User getReviewedBy() { return reviewedBy; }
}
