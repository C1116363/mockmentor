package com.learn.interviewmentor.model;

/**
 * Where a mentor is in the onboarding process.
 *
 * INCOMPLETE -> signed up, but hasn't filled in their profile yet
 * PENDING    -> submitted, waiting for an admin to check it
 * APPROVED   -> verified, can take interviews
 * REJECTED   -> admin turned it down; they can fix it and resubmit
 *
 * Only APPROVED mentors can see the queue or accept an interview. That check
 * lives on the server, not in the browser.
 */
public enum VerificationStatus {
    INCOMPLETE,
    PENDING,
    APPROVED,
    REJECTED
}
