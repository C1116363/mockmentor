package com.learn.interviewmentor.model;

/**
 * OPEN      -> declared by the mentor, nobody assigned to it yet
 * BOOKED    -> an admin has mapped a student's booking onto this slot
 * WITHDRAWN -> the mentor took it back before anyone was assigned
 *
 * WITHDRAWN rather than deleting the row: "I was free and pulled out" and "I was
 * never free" are different things, and only one of them is worth an admin
 * knowing about when a slot they were counting on disappears.
 */
public enum AvailabilityStatus {
    OPEN,
    BOOKED,
    WITHDRAWN
}
