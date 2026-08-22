package com.learn.interviewmentor.meeting;

import com.learn.interviewmentor.model.InterviewRequest;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Real Google Meet links - NOT IMPLEMENTED, and deliberately fails loudly.
 *
 * Turn it on with app.meeting.provider=google and this throws on the first
 * assignment, rather than silently handing people a dead link.
 *
 * To make it work you need:
 *
 *   1. A Google Cloud project with the Google Calendar API enabled.
 *   2. Either a service account with domain-wide delegation (Workspace only),
 *      or an OAuth2 flow where an organiser account grants calendar access.
 *   3. The google-api-client + google-api-services-calendar dependencies.
 *   4. Insert a Calendar event with a conferenceData request:
 *
 *        Event event = new Event()
 *            .setSummary("Mock interview: " + request.getTopic())
 *            .setStart(...).setEnd(...)
 *            .setAttendees(List.of(
 *                new EventAttendee().setEmail(request.getStudent().getEmail()),
 *                new EventAttendee().setEmail(request.getMentor().getEmail())))
 *            .setConferenceData(new ConferenceData().setCreateRequest(
 *                new CreateConferenceRequest()
 *                    .setRequestId(UUID.randomUUID().toString())
 *                    .setConferenceSolutionKey(
 *                        new ConferenceSolutionKey().setType("hangoutsMeet"))));
 *
 *        Event created = calendar.events()
 *            .insert("primary", event)
 *            .setConferenceDataVersion(1)
 *            .execute();
 *
 *        return created.getHangoutLink();
 *
 * That also puts the interview in both calendars and emails them an invite,
 * which is the real reason to do it this way.
 */
@Component
@ConditionalOnProperty(name = "app.meeting.provider", havingValue = "google")
public class GoogleMeetLinkGenerator implements MeetingLinkGenerator {

    @Override
    public String generateFor(InterviewRequest request) {
        throw new UnsupportedOperationException(
                "Google Meet links need the Google Calendar API with OAuth credentials, "
                        + "which are not configured. Set app.meeting.provider=jitsi, or paste a "
                        + "meeting link in manually when assigning. See the class comment for "
                        + "what implementing this properly involves.");
    }
}
