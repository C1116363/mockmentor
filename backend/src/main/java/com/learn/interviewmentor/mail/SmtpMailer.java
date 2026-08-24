package com.learn.interviewmentor.mail;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

/**
 * Real email, over SMTP. Works with Gmail.
 *
 * <h2>Gmail needs an App Password, not your Google password</h2>
 * Google removed plain-password SMTP ("less secure apps") in 2022, so the
 * account password will simply be rejected. What works:
 *
 * <ol>
 *   <li>Turn on 2-Step Verification on the Google account - App Passwords do
 *       not exist until you do.</li>
 *   <li>myaccount.google.com -> Security -> App passwords -> generate one.</li>
 *   <li>Put the 16 characters in MAIL_PASSWORD. Google displays it in four
 *       groups for readability; the spaces are not part of it.</li>
 * </ol>
 *
 * <h2>What Gmail is and is not good for</h2>
 * Free, instant to set up, and capped around 500 messages a day - fine for
 * password resets at this size. It is not a bulk sender: mail goes out as your
 * personal address rather than your domain, so it lands in spam more often, and
 * Google will lock the account if it decides you are sending marketing. When
 * that starts mattering, a transactional provider is a drop-in replacement -
 * only the four properties change, not this class.
 */
@Component
@ConditionalOnProperty(name = "app.mail.provider", havingValue = "smtp")
public class SmtpMailer implements Mailer {

    private static final Logger log = LoggerFactory.getLogger(SmtpMailer.class);

    private final JavaMailSender sender;
    private final String from;

    public SmtpMailer(JavaMailSender sender,
                      @Value("${app.mail.from:}") String from,
                      @Value("${spring.mail.username:}") String username) {
        this.sender = sender;
        // Gmail rewrites From to the authenticated account anyway, so defaulting
        // to the username avoids a silently wrong header when app.mail.from is
        // left unset.
        this.from = from.isBlank() ? username : from;

        if (this.from.isBlank()) {
            log.warn("app.mail.provider=smtp but neither app.mail.from nor "
                    + "spring.mail.username is set - mail will almost certainly be rejected.");
        }
    }

    @Override
    public String describe() {
        return "SMTP as " + from;
    }

    /**
     * @implNote Plain text, not HTML.
     *
     * A password-reset mail is one sentence and a link. HTML would buy nothing
     * and cost something real: HTML mail is filtered more aggressively, and a
     * link whose text differs from its href is exactly the shape of a phishing
     * mail - which is a bad habit to teach users about your own emails.
     *
     * @implNote Failures are swallowed on purpose. See {@link Mailer}: the
     * caller cannot report this one without also revealing whether the address
     * exists.
     */
    @Override
    public void send(String to, String subject, String body) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(to);
        message.setSubject(subject);
        message.setText(body);

        try {
            sender.send(message);
            log.info("Sent '{}' to {}", subject, to);
        } catch (Exception e) {
            // Logged with the address so it can be chased, and with the cause
            // because "Username and Password not accepted" versus "connection
            // timed out" are completely different problems.
            log.error("Could not send '{}' to {}", subject, to, e);
        }
    }
}
