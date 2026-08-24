package com.learn.interviewmentor.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * One outstanding "reset my password" request.
 *
 * <h2>The column is a hash, and that is the point of the class</h2>
 * {@link #tokenHash} holds SHA-256 of the token, never the token. The plain
 * value exists exactly once - in the email - and is never written down here.
 *
 * The reason is the failure you are guarding against. A database is read far
 * more often than it is meant to be: a backup on a laptop, a support query, a
 * SQL injection somewhere unrelated, a dump shared with a contractor. If this
 * column held the real tokens, any of those hands over a working password reset
 * for every account with an open request. Holding hashes makes the table
 * useless to anyone who reads it, because the email is the only place the
 * usable value ever existed.
 *
 * <h2>Why no salt or bcrypt</h2>
 * Deliberate, and the opposite of the rule for passwords. This token is 256
 * bits of output from a CSPRNG, so it has no pattern to guess and no dictionary
 * to try - the slow hashing that protects a human-chosen password buys nothing
 * here, and would add a bcrypt round to every lookup. A plain SHA-256 of a
 * random 256-bit value is not brute-forceable.
 */
@Entity
@Table(name = "password_reset_tokens", indexes = {
        // Every redemption looks up by this. Unique because two rows with the
        // same hash would mean the same token opens two accounts.
        @Index(name = "idx_reset_token_hash", columnList = "token_hash", unique = true),
        // "How many has this user asked for recently?" - the rate limit.
        @Index(name = "idx_reset_user", columnList = "user_id")
})
public class PasswordResetToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** SHA-256 of the token, hex. Never the token itself. */
    @Column(name = "token_hash", nullable = false, length = 64, unique = true)
    private String tokenHash;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    /**
     * When it was redeemed. Null while still usable.
     *
     * A used token is kept rather than deleted, so that "this link has already
     * been used" can be told apart from "this link never existed". Both are
     * refused, but only one of them means somebody should worry.
     */
    @Column(name = "used_at")
    private LocalDateTime usedAt;

    /**
     * Set when a later request supersedes this one, or when the password
     * changes by any other route. Separate from usedAt because nobody clicked
     * it - and a token that was quietly cancelled tells a different story
     * during an incident than one that was spent.
     */
    @Column(name = "invalidated_at")
    private LocalDateTime invalidatedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected PasswordResetToken() {
        // JPA
    }

    public PasswordResetToken(String tokenHash, User user, LocalDateTime expiresAt) {
        this.tokenHash = tokenHash;
        this.user = user;
        this.expiresAt = expiresAt;
    }

    @PrePersist
    void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    /** Unused, uncancelled, and still inside its window. */
    public boolean isUsable() {
        return usedAt == null
                && invalidatedAt == null
                && expiresAt.isAfter(LocalDateTime.now());
    }

    public void markUsed() {
        this.usedAt = LocalDateTime.now();
    }

    public void invalidate() {
        if (this.usedAt == null && this.invalidatedAt == null) {
            this.invalidatedAt = LocalDateTime.now();
        }
    }

    public Long getId() { return id; }
    public String getTokenHash() { return tokenHash; }
    public User getUser() { return user; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public LocalDateTime getUsedAt() { return usedAt; }
    public LocalDateTime getInvalidatedAt() { return invalidatedAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
