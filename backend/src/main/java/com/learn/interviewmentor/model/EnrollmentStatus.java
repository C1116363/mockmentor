package com.learn.interviewmentor.model;

/**
 * A student's progress through buying one plan. Same shape as the interview
 * payment flow, because it is the same manual UPI process.
 *
 * AWAITING_PAYMENT -> student picked the plan, hasn't sent proof yet
 * SUBMITTED        -> UPI reference + screenshot sent, an admin needs to check
 * ACTIVE           -> admin confirmed the money; access runs until expiresAt
 * REJECTED         -> admin couldn't match the payment; the student can resend
 * CANCELLED        -> student backed out before paying
 * EXPIRED          -> the access window ran out
 */
public enum EnrollmentStatus {
    AWAITING_PAYMENT,
    SUBMITTED,
    ACTIVE,
    REJECTED,
    CANCELLED,
    EXPIRED
}
