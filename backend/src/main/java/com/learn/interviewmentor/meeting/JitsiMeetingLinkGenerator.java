package com.learn.interviewmentor.meeting;

import com.learn.interviewmentor.model.InterviewRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;

/**
 * The default provider: a Jitsi Meet room.
 *
 * Why not Google Meet? Because you cannot mint a real meet.google.com link
 * without going through the Google Calendar API with OAuth credentials - there
 * is no public endpoint that just hands you one. Generating a meet.google.com
 * URL ourselves would produce a string that *looks* right and fails the moment
 * somebody clicks it.
 *
 * Jitsi rooms, by contrast, exist the instant someone opens the URL. No account,
 * no API key, and both people land in the same call - which is exactly the
 * behaviour we want. See GoogleMeetLinkGenerator for the real-Meet path.
 *
 * The room name is long and random. A Jitsi room is reachable by anyone who
 * knows its name, so a guessable one (say "interview-7") would let strangers
 * walk into somebody's interview.
 */
@Component
@ConditionalOnProperty(name = "app.meeting.provider", havingValue = "jitsi", matchIfMissing = true)
public class JitsiMeetingLinkGenerator implements MeetingLinkGenerator {

    private static final String ALPHABET = "abcdefghijkmnopqrstuvwxyz23456789";
    private static final int SEGMENT = 6;
    private static final int SEGMENTS = 3;

    private final SecureRandom random = new SecureRandom();
    private final String baseUrl;

    public JitsiMeetingLinkGenerator(@Value("${app.meeting.base-url}") String baseUrl) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }

    @Override
    public String generateFor(InterviewRequest request) {
        StringBuilder room = new StringBuilder("confirmplacement");
        for (int s = 0; s < SEGMENTS; s++) {
            room.append('-');
            for (int i = 0; i < SEGMENT; i++) {
                room.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
            }
        }
        return baseUrl + "/" + room;
    }
}
