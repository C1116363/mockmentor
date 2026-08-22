package com.learn.interviewmentor.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

/**
 * The extra details that only a MENTOR has. Kept out of User so students and
 * admins don't carry a pile of null columns.
 *
 * This is a @OneToOne: one user <-> one mentor profile. The foreign key
 * (user_id) lives on this table, which makes MentorProfile the owning side.
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

    /** e.g. "Java, Spring Boot, System Design" */
    @Column(nullable = false)
    private String expertise;

    @Column(name = "years_of_experience", nullable = false)
    private int yearsOfExperience;

    @Column(name = "current_company")
    private String currentCompany;

    @Column(length = 500)
    private String bio;

    protected MentorProfile() {
        // JPA
    }

    public MentorProfile(User user, String expertise, int yearsOfExperience, String currentCompany, String bio) {
        this.user = user;
        this.expertise = expertise;
        this.yearsOfExperience = yearsOfExperience;
        this.currentCompany = currentCompany;
        this.bio = bio;
    }

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public String getExpertise() {
        return expertise;
    }

    public void setExpertise(String expertise) {
        this.expertise = expertise;
    }

    public int getYearsOfExperience() {
        return yearsOfExperience;
    }

    public void setYearsOfExperience(int yearsOfExperience) {
        this.yearsOfExperience = yearsOfExperience;
    }

    public String getCurrentCompany() {
        return currentCompany;
    }

    public void setCurrentCompany(String currentCompany) {
        this.currentCompany = currentCompany;
    }

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }
}
