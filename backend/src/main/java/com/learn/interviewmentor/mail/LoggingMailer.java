package com.learn.interviewmentor.mail;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Writes the email to the server log instead of sending it.
 *
 * <h2>The default, and deliberately so</h2>
 * Forgot-password works on a fresh clone with no Gmail account, no app
 * password and no configuration - you copy the reset link out of the backend
 * console. That matters more than it sounds: the alternative is a feature that
 * cannot be tested until somebody has finished a Google security setup, which
 * in practice means it does not get tested.
 *
 * <h2>Not for production</h2>
 * The link in the log is a working password reset for a real account, so on a
 * real server this must be swapped for SMTP - anyone who can read the logs can
 * otherwise take over any account by asking for a reset. The startup banner
 * says so out loud rather than leaving it to be noticed.
 */
@Component
@ConditionalOnProperty(name = "app.mail.provider", havingValue = "log", matchIfMissing = true)
public class LoggingMailer implements Mailer {

    private static final Logger log = LoggerFactory.getLogger(LoggingMailer.class);

    public LoggingMailer() {
        log.warn("""

                ------------------------------------------------------------------
                Email is NOT being sent. app.mail.provider=log
                Password-reset emails are printed to this console instead.
                Fine for development. On a real server set app.mail.provider=smtp,
                because a reset link in a log file is a working account takeover.
                ------------------------------------------------------------------""");
    }

    @Override
    public String describe() {
        return "console (no email is actually sent)";
    }

    @Override
    public void send(String to, String subject, String body) {
        log.info("""

                ================= EMAIL (not sent - console only) =================
                To:      {}
                Subject: {}

                {}
                ===================================================================""",
                to, subject, body);
    }
}
