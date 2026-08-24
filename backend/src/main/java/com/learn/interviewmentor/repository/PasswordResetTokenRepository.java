package com.learn.interviewmentor.repository;

import com.learn.interviewmentor.model.PasswordResetToken;
import com.learn.interviewmentor.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    Optional<PasswordResetToken> findByTokenHash(String tokenHash);

    /**
     * How many resets this account has asked for since a given moment.
     *
     * The rate limit. Without one, this endpoint is a free email-bombing
     * service pointed at any address somebody types - and the victim is not
     * even a user of this app, which is what makes it worth caring about.
     */
    long countByUserAndCreatedAtAfter(User user, LocalDateTime since);

    /** Every still-live token for one account. */
    @Query("""
            select t from PasswordResetToken t
            where t.user = :user and t.usedAt is null and t.invalidatedAt is null
            """)
    List<PasswordResetToken> findLiveTokensFor(@Param("user") User user);

    /**
     * Housekeeping: drop rows that can never be used again.
     *
     * Spent and expired tokens are worthless - the hash is not reversible and
     * the row is refused on every check - so this is tidiness rather than
     * security. Kept for a while regardless, because "when did they reset it?"
     * is a real support question.
     */
    @Modifying
    @Query("delete from PasswordResetToken t where t.createdAt < :before")
    int deleteOlderThan(@Param("before") LocalDateTime before);
}
