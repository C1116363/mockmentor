package com.learn.interviewmentor.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * Every person who can log in - student, mentor or admin.
 *
 * Note this is a plain JPA entity, NOT Spring Security's UserDetails. Mixing the
 * two is a classic beginner trap: your database model and the security model
 * should stay separate. CustomUserDetailsService is what bridges them.
 */
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Column(nullable = false, unique = true)
    private String email;

    /** BCrypt hash - never the plain password. */
    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;

    /** An admin can switch this off to block someone from logging in. */
    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected User() {
        // JPA
    }

    public User(String fullName, String email, String password, Role role) {
        this.fullName = fullName;
        this.email = email;
        this.password = password;
        this.role = role;
        this.active = true;
    }

    @PrePersist
    void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public String getPassword() {
        return password;
    }

    /**
     * When the password was last changed by a reset.
     *
     * Exists so that resetting a password actually logs other sessions out.
     * The JWT is stateless and lives 24 hours, so without this a stolen token
     * keeps working for a day after the victim has changed their password -
     * which makes the reset feel like it worked while the attacker is still
     * inside. JwtAuthenticationFilter refuses any token issued before this
     * moment.
     *
     * Null for accounts that have never reset, which is why the check is
     * null-tolerant rather than defaulting this to the creation time: setting
     * it at signup would risk rejecting the token issued moments later by the
     * signup's own auto-login.
     */
    @Column(name = "password_changed_at")
    private LocalDateTime passwordChangedAt;

    public LocalDateTime getPasswordChangedAt() {
        return passwordChangedAt;
    }

    /**
     * Change the password and cut every existing session loose.
     *
     * Used by the reset flow instead of setPassword, so the two halves cannot
     * drift apart - a reset that forgot to stamp the time would silently leave
     * old tokens valid, and nothing would look wrong.
     */
    public void changePassword(String encodedPassword) {
        this.password = encodedPassword;
        this.passwordChangedAt = LocalDateTime.now();
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
