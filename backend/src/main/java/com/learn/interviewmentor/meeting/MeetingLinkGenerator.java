package com.learn.interviewmentor.meeting;

import com.learn.interviewmentor.model.InterviewRequest;

/**
 * Creates the video-call link for an interview.
 *
 * Behind an interface on purpose: which provider you use is a deployment
 * decision, not a business rule. Nothing in the service layer knows or cares
 * whether the room is on Jitsi, Google Meet or Zoom.
 */
public interface MeetingLinkGenerator {

    /** A joinable room URL for this interview. */
    String generateFor(InterviewRequest request);
}
