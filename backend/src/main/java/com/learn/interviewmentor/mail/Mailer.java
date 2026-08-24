package com.learn.interviewmentor.mail;

/**
 * Sending an email, behind an interface.
 *
 * Same reasoning as MeetingLinkGenerator, CollaboratorGranter and
 * PaymentGateway: which channel actually delivers the mail is a deployment
 * decision, and no business rule should be able to tell the difference.
 *
 * <h2>Nothing here throws on a delivery failure</h2>
 * Not because failures do not matter, but because of who is waiting. The only
 * caller is the forgot-password flow, and it must answer identically whether
 * the address exists or not - so it cannot report "we could not send that"
 * without also reporting "that address is real". A failure is logged loudly
 * and the caller is told nothing, which is the correct trade for this feature
 * and would be the wrong one for, say, an invoice.
 */
public interface Mailer {

    /** Shown at startup so it is obvious which one is wired in. */
    String describe();

    /**
     * @param to      recipient address
     * @param subject plain text
     * @param body    plain text. Not HTML - see LoggingMailer for why that is
     *                deliberate rather than unfinished.
     */
    void send(String to, String subject, String body);
}
