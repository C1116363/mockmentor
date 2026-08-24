package com.learn.interviewmentor.service.impl;

import com.learn.interviewmentor.dto.auth.ForgotPasswordDto;
import com.learn.interviewmentor.dto.auth.ResetPasswordDto;
import com.learn.interviewmentor.exception.BadRequestException;
import com.learn.interviewmentor.mail.Mailer;
import com.learn.interviewmentor.model.PasswordResetToken;
import com.learn.interviewmentor.model.User;
import com.learn.interviewmentor.repository.PasswordResetTokenRepository;
import com.learn.interviewmentor.repository.UserRepository;
import com.learn.interviewmentor.service.PasswordResetService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Forgotten passwords.
 *
 * Read {@link PasswordResetService} first for the one rule that shapes the
 * flow. This class is the rest of the decisions, each of which is a place these
 * features are commonly got wrong.
 */
@Service
@Transactional(readOnly = true)
public class PasswordResetServiceImpl implements PasswordResetService {

    private static final Logger log = LoggerFactory.getLogger(PasswordResetServiceImpl.class);

    /**
     * How long a link works.
     *
     * Short enough that a link left sitting in an inbox, or in a mailbox
     * somebody else later gains access to, is usually already dead. Long enough
     * that "I'll do it when I get back to my desk" still works. Thirty minutes
     * is the common choice and there is no cleverer answer.
     */
    private static final int VALID_MINUTES = 30;

    /**
     * Requests allowed per account per hour.
     *
     * The limit is per account rather than per IP because the harm being
     * prevented is landing in somebody's inbox, and that is chosen by the
     * address in the body, not by where the request came from. Five leaves room
     * for the genuine "it didn't arrive, try again" while making this useless
     * as a way to flood a mailbox.
     */
    private static final int MAX_PER_HOUR = 5;

    /**
     * 32 bytes of CSPRNG output.
     *
     * 256 bits, so guessing is not a threat model. Note SecureRandom, not
     * Random and not UUID.randomUUID().toString() - Random is a predictable
     * sequence from a 48-bit seed, and predicting the next token means taking
     * over the next account that asks for a reset.
     */
    private static final int TOKEN_BYTES = 32;

    private static final SecureRandom RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final Mailer mailer;
    private final String appUrl;

    public PasswordResetServiceImpl(UserRepository userRepository,
                                    PasswordResetTokenRepository tokenRepository,
                                    PasswordEncoder passwordEncoder,
                                    Mailer mailer,
                                    @Value("${app.frontend-url}") String appUrl) {
        this.userRepository = userRepository;
        this.tokenRepository = tokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.mailer = mailer;
        // Trailing slash trimmed here so the link builder does not have to care,
        // and so a stray one in .env cannot produce a double slash in the link.
        this.appUrl = appUrl.endsWith("/") ? appUrl.substring(0, appUrl.length() - 1) : appUrl;

        log.info("Password reset: links valid {} minutes, max {}/hour, delivery via {}",
                VALID_MINUTES, MAX_PER_HOUR, mailer.describe());
    }

    // ------------------------------------------------------------------
    // Asking for a link
    // ------------------------------------------------------------------

    @Transactional
    @Override
    public void requestReset(ForgotPasswordDto dto) {
        String email = normalise(dto.email());

        Optional<User> found = userRepository.findByEmailIgnoreCase(email);

        if (found.isEmpty()) {
            // The whole point. No exception, no different message, no different
            // status - the caller cannot tell this apart from a success, so it
            // cannot be used to find out who has an account here.
            log.info("Password reset asked for an address with no account");
            return;
        }

        User user = found.get();

        if (tokenRepository.countByUserAndCreatedAtAfter(user, LocalDateTime.now().minusHours(1))
                >= MAX_PER_HOUR) {
            // Also silent, and for the same reason: a "slow down" reply would
            // itself confirm the address exists. Logged so a real flood is
            // visible from this side.
            log.warn("Password reset rate limit hit for {}", user.getEmail());
            return;
        }

        // Anything already outstanding stops working the moment a new link is
        // issued. Without this, asking three times leaves three live tokens,
        // and the two the user abandoned are still working keys - including the
        // one in the email they just deleted as suspicious.
        List<PasswordResetToken> live = tokenRepository.findLiveTokensFor(user);
        live.forEach(PasswordResetToken::invalidate);

        String token = newToken();
        tokenRepository.save(new PasswordResetToken(
                sha256(token), user, LocalDateTime.now().plusMinutes(VALID_MINUTES)));

        mailer.send(user.getEmail(), "Reset your ConfirmPlacement password",
                emailBody(user, token));

        log.info("Password reset link issued for {} ({} superseded)", user.getEmail(), live.size());
    }

    // ------------------------------------------------------------------
    // Redeeming one
    // ------------------------------------------------------------------

    @Transactional
    @Override
    public void resetPassword(ResetPasswordDto dto) {
        // Looked up by hash, because that is all that was ever stored.
        PasswordResetToken record = tokenRepository.findByTokenHash(sha256(dto.token()))
                .orElseThrow(PasswordResetServiceImpl::rejected);

        if (!record.isUsable()) {
            // Expired, already spent, or superseded by a newer request - all
            // one message. Telling them which would confirm the token was real,
            // which is the one fact somebody guessing at tokens wants.
            log.info("Refused a reset token for {} (used={}, invalidated={}, expires={})",
                    record.getUser().getEmail(), record.getUsedAt(),
                    record.getInvalidatedAt(), record.getExpiresAt());
            throw rejected();
        }

        User user = record.getUser();

        // changePassword, not setPassword: it stamps passwordChangedAt, which
        // is what makes every existing JWT for this account stop working. A
        // reset that leaves the attacker's session alive is not a reset.
        user.changePassword(passwordEncoder.encode(dto.newPassword()));
        userRepository.save(user);

        record.markUsed();

        // Belt and braces. The user's other live tokens are already invalid in
        // practice because one was just spent, but leaving them merely unused
        // means a second link from the same hour still works - and the person
        // most likely to have that link is whoever prompted the reset.
        tokenRepository.findLiveTokensFor(user).forEach(PasswordResetToken::invalidate);

        log.info("Password reset completed for {} - all existing sessions invalidated",
                user.getEmail());
    }

    @Transactional
    @Override
    public int purgeExpired() {
        // Kept a fortnight past creation. They are unusable long before that;
        // the delay is so "when did they reset it?" is still answerable during
        // the window when somebody would actually ask.
        return tokenRepository.deleteOlderThan(LocalDateTime.now().minusDays(14));
    }

    // ------------------------------------------------------------------

    /**
     * One message for every way a token can fail.
     *
     * Unknown, expired, spent and superseded are indistinguishable to the
     * caller. The next step is the same in all four cases, so the message says
     * that instead of diagnosing.
     */
    private static BadRequestException rejected() {
        return new BadRequestException(
                "That reset link is no longer valid. Links expire after "
                        + VALID_MINUTES + " minutes and can only be used once - "
                        + "ask for a new one.");
    }

    /** URL-safe, no padding: it has to survive being pasted out of an email. */
    private static String newToken() {
        byte[] bytes = new byte[TOKEN_BYTES];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 is mandatory in every JVM. If it is genuinely missing,
            // failing loudly beats falling back to something weaker.
            throw new IllegalStateException("SHA-256 is unavailable", e);
        }
    }

    /** Addresses are stored lower-cased; matching has to agree. */
    private static String normalise(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    /**
     * The link points at the frontend, not the API.
     *
     * A reset link is clicked by a person, so it has to land on a page with a
     * password box. Sending them to the backend would show raw JSON - and worse,
     * would put the token in a URL the API logs.
     */
    private String emailBody(User user, String token) {
        String link = appUrl + "/?reset=" + URLEncoder.encode(token, StandardCharsets.UTF_8);

        return """
                Hi %s,

                Someone asked to reset the password for your ConfirmPlacement account.
                Open this link to choose a new one:

                %s

                The link works for %d minutes and can only be used once.

                If this wasn't you, you can ignore this email - your password has not
                been changed, and nobody can get in without this link.

                - ConfirmPlacement
                """.formatted(user.getFullName(), link, VALID_MINUTES);
    }
}
