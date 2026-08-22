package com.learn.interviewmentor.model;

/**
 * Lifecycle of an interview request.
 *
 * AWAITING_PAYMENT -> slot is held, but the money hasn't been confirmed yet.
 *                     Mentors never see these.
 * PENDING   -> paid and verified, nobody has picked it up yet
 * SCHEDULED -> a mentor accepted it and set a date + meeting link
 * COMPLETED -> the interview happened, mentor left feedback
 * CANCELLED -> student or mentor called it off
 */
public enum RequestStatus {
    AWAITING_PAYMENT,
    PENDING,
    SCHEDULED,
    COMPLETED,
    CANCELLED
}
