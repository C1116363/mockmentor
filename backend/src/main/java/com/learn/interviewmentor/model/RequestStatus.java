package com.learn.interviewmentor.model;

/**
 * Lifecycle of an interview request.
 *
 * PENDING   -> student asked, nobody picked it up yet
 * SCHEDULED -> a mentor accepted it and set a date + meeting link
 * COMPLETED -> the interview happened, mentor left feedback
 * CANCELLED -> student or mentor called it off
 */
public enum RequestStatus {
    PENDING,
    SCHEDULED,
    COMPLETED,
    CANCELLED
}
